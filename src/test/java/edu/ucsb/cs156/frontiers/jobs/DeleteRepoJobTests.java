package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.errors.JobCancelledException;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
public class DeleteRepoJobTests {

  @Mock private CourseRepository courseRepository;
  @Mock private RepositoryService repositoryService;

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  /** Every wait the job asked for, in milliseconds; the job is given this so tests never wait. */
  List<Long> sleeps = new ArrayList<>();

  Course course =
      Course.builder()
          .id(1L)
          .courseName("Test Course")
          .orgName("test-org")
          .installationId("123")
          .build();

  private DeleteRepoJob job(String prefix) {
    return DeleteRepoJob.builder()
        .courseId(1L)
        .prefix(prefix)
        .courseRepository(courseRepository)
        .repositoryService(repositoryService)
        .sleeper(sleeps::add)
        .build();
  }

  private static List<GithubRepository> repos(String... names) {
    List<GithubRepository> repos = new ArrayList<>();
    for (String name : names) {
      repos.add(new GithubRepository(name, "test-org/" + name));
    }
    return repos;
  }

  private static HttpClientErrorException githubError(HttpStatus status, String body) {
    return HttpClientErrorException.create(
        status,
        status.getReasonPhrase(),
        new HttpHeaders(),
        body.getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);
  }

  @Test
  public void test_getScope_returnsCourseScope() {
    DeleteRepoJob job = DeleteRepoJob.builder().courseId(1L).build();

    assertEquals("course", job.getScopeType());
    assertEquals(1L, job.getScopeId());
  }

  @Test
  public void test_defaultSleeper_reallyWaits() throws Exception {
    DeleteRepoJob job = DeleteRepoJob.builder().courseId(1L).build();

    long start = System.nanoTime();
    job.sleeper.sleep(20);
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

    assertTrue(elapsedMillis >= 20, "slept only " + elapsedMillis + " ms");
  }

  @Test
  public void test_accept_courseNotFound() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    job("lab01").accept(ctx);

    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        ERROR: Course with ID 1 not found""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_courseWithoutGithubOrg() throws Exception {
    Course noOrg = Course.builder().id(1L).courseName("Test Course").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(noOrg));

    job("lab01").accept(ctx);

    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        ERROR: Course has no linked GitHub organization""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_courseWithOrgNameButNoInstallationId() throws Exception {
    Course noInstallation =
        Course.builder().id(1L).courseName("Test Course").orgName("test-org").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(noInstallation));

    job("lab01").accept(ctx);

    verify(repositoryService, never()).getRepositoriesMatchingPrefix(any(), any());
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        ERROR: Course has no linked GitHub organization""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_noReposMatchPrefix() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01")).thenReturn(repos());

    job("lab01").accept(ctx);

    verify(repositoryService, never()).deleteRepositoryIfEmpty(any(), any());
    assertEquals(List.of(), sleeps);
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        0 repos found with prefix lab01
        0 repos deleted
        0 repos retained
        0 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_allReposEmpty_deletesEachAndWaitsBetweenThem() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenReturn(repos("lab01-student1", "lab01-student2", "lab01-student3"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student3")).thenReturn(true);

    job("lab01").accept(ctx);

    // one wait between each pair of repos: none before the first or after the last
    assertEquals(List.of(1000L, 1000L), sleeps);
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        3 repos found with prefix lab01
        Repo lab01-student1 deleted.
        Repo lab01-student2 deleted.
        Repo lab01-student3 deleted.
        3 repos deleted
        0 repos retained
        0 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_reposWithCommitsAreRetained() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenReturn(repos("lab01-student1", "lab01-student2"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(false);

    job("lab01").accept(ctx);

    assertEquals(List.of(1000L), sleeps);
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        2 repos found with prefix lab01
        Repo lab01-student1 deleted.
        Repo lab01-student2 not deleted; commits exist.
        1 repos deleted
        1 repos retained
        0 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_githubErrorOnOneRepo_logsGithubMessageAndContinues() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenReturn(repos("lab01-student1", "lab01-student2"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"))
        .thenThrow(
            githubError(
                HttpStatus.FORBIDDEN, "{\"message\":\"Must have admin rights to Repository.\"}"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(true);

    job("lab01").accept(ctx);

    assertEquals(List.of(1000L), sleeps);
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        2 repos found with prefix lab01
        Repo lab01-student1 not deleted; error: 403 FORBIDDEN \
        {"message":"Must have admin rights to Repository."}
        Repo lab01-student2 deleted.
        1 repos deleted
        0 repos retained
        1 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_nonHttpErrorOnOneRepo_logsExceptionMessageAndContinues()
      throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenReturn(repos("lab01-student1", "lab01-student2"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1"))
        .thenThrow(new RuntimeException("connection reset"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student2")).thenReturn(false);

    job("lab01").accept(ctx);

    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        2 repos found with prefix lab01
        Repo lab01-student1 not deleted; error: connection reset
        Repo lab01-student2 not deleted; commits exist.
        0 repos deleted
        1 repos retained
        1 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_githubErrorWhileListing() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenThrow(githubError(HttpStatus.NOT_FOUND, "{\"message\":\"Not Found\"}"));

    job("lab01").accept(ctx);

    verify(repositoryService, never()).deleteRepositoryIfEmpty(any(), any());
    assertEquals(List.of(), sleeps);
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        ERROR: Failed to list repositories: 404 NOT_FOUND {"message":"Not Found"}
        0 repos deleted
        0 repos retained
        1 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_nonHttpErrorWhileListing() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenThrow(new RuntimeException("connection reset"));

    job("lab01").accept(ctx);

    verify(repositoryService, never()).deleteRepositoryIfEmpty(any(), any());
    assertEquals(
        """
        Starting delete repository job for course ID: 1
        prefix=lab01
        ERROR: Failed to list repositories: connection reset
        0 repos deleted
        0 repos retained
        1 errors
        Done""",
        jobStarted.getLog());
  }

  @Test
  public void test_accept_cancellationIsNotSwallowedAsARepoError() throws Exception {
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(repositoryService.getRepositoriesMatchingPrefix(course, "lab01"))
        .thenReturn(repos("lab01-student1", "lab01-student2"));
    when(repositoryService.deleteRepositoryIfEmpty(course, "lab01-student1")).thenReturn(true);

    // ctx.log() is where lib-jobs notices a cancel request; simulate one arriving there. Lenient,
    // since every other log() call, with other messages, should do nothing.
    JobContext cancellingCtx = mock(JobContext.class);
    lenient()
        .doThrow(new JobCancelledException(1L))
        .when(cancellingCtx)
        .log("Repo lab01-student1 deleted.");

    assertThrows(JobCancelledException.class, () -> job("lab01").accept(cancellingCtx));

    verify(repositoryService, never()).deleteRepositoryIfEmpty(course, "lab01-student2");
  }
}
