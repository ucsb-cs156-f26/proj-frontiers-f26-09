package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.RepositoryService.GithubRepository;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Deletes the empty repositories (those with no commits) in a course's GitHub organization whose
 * names start with a given prefix, so that repositories created by mistake (for example, with a
 * typo in the prefix) can be cleaned up. A repository that has any commits is never deleted.
 */
@Builder
public class DeleteRepoJob implements JobContextConsumer {

  /**
   * Milliseconds to wait between repositories. Each repository takes a GitHub request to check it
   * for commits, and maybe one to delete it; GitHub asks integrations that make many such requests
   * to wait at least a second between them, to stay under its secondary rate limits.
   */
  public static final long DELAY_BETWEEN_REPOS_MILLIS = 1000;

  /** Waits for the given number of milliseconds; replaceable so that tests do not really wait. */
  @FunctionalInterface
  public interface Sleeper {
    void sleep(long milliseconds) throws InterruptedException;
  }

  Long courseId;
  String prefix;
  CourseRepository courseRepository;
  RepositoryService repositoryService;
  @Builder.Default Sleeper sleeper = Thread::sleep;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return courseId;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(String.format("Starting delete repository job for course ID: %s", courseId));
    ctx.log(String.format("prefix=%s", prefix));

    Optional<Course> courseOpt = courseRepository.findById(courseId);
    if (courseOpt.isEmpty()) {
      ctx.log(String.format("ERROR: Course with ID %s not found", courseId));
      return;
    }

    Course course = courseOpt.get();
    if (course.getOrgName() == null || course.getInstallationId() == null) {
      ctx.log("ERROR: Course has no linked GitHub organization");
      return;
    }

    int reposDeleted = 0;
    int reposRetained = 0;
    int repoErrors = 0;

    List<GithubRepository> repositories;
    try {
      repositories = repositoryService.getRepositoriesMatchingPrefix(course, prefix);
    } catch (Exception e) {
      repoErrors++;
      ctx.log(String.format("ERROR: Failed to list repositories: %s", describeError(e)));
      logSummary(ctx, reposDeleted, reposRetained, repoErrors);
      return;
    }

    ctx.log(String.format("%d repos found with prefix %s", repositories.size(), prefix));

    for (int i = 0; i < repositories.size(); i++) {
      if (i > 0) {
        sleeper.sleep(DELAY_BETWEEN_REPOS_MILLIS);
      }
      String name = repositories.get(i).name();

      // Only the GitHub calls are in the try: ctx.log() is a cancellation checkpoint, and the
      // exception it throws when the job is cancelled must not be caught and counted as a repo
      // error.
      boolean deleted;
      try {
        deleted = repositoryService.deleteRepositoryIfEmpty(course, name);
      } catch (Exception e) {
        repoErrors++;
        ctx.log(String.format("Repo %s not deleted; error: %s", name, describeError(e)));
        continue;
      }

      if (deleted) {
        reposDeleted++;
        ctx.log(String.format("Repo %s deleted.", name));
      } else {
        reposRetained++;
        ctx.log(String.format("Repo %s not deleted; commits exist.", name));
      }
    }

    logSummary(ctx, reposDeleted, reposRetained, repoErrors);
  }

  private static void logSummary(
      JobContext ctx, int reposDeleted, int reposRetained, int repoErrors) {
    ctx.log(String.format("%d repos deleted", reposDeleted));
    ctx.log(String.format("%d repos retained", reposRetained));
    ctx.log(String.format("%d errors", repoErrors));
    ctx.log("Done");
  }

  /**
   * Describes an error for the job log. For an error response from GitHub this is its status and
   * body, which holds GitHub's own message saying what went wrong.
   */
  private static String describeError(Exception e) {
    if (e instanceof HttpStatusCodeException httpError) {
      return httpError.getStatusCode() + " " + httpError.getResponseBodyAsString();
    }
    return e.getMessage();
  }
}
