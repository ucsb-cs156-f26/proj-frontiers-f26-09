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

    List<GithubRepository> repositories;
    int errors = 0;
    try {
      repositories = repositoryService.getRepositoriesMatchingPrefix(course, prefix);
    } catch (Exception e) {
      ctx.log(String.format("ERROR: Failed to list repositories: %s", e.getMessage()));
      errors++;
      ctx.log(String.format("Summary: found=0, deleted=0, retained=0, errors=%d", errors));
      return;
    }

    ctx.log(String.format("%d repos found with prefix %s", repositories.size(), prefix));

    int deleted = 0;
    int retained = 0;
    boolean firstRepository = true;

    for (GithubRepository repository : repositories) {
      if (!firstRepository) {
        sleeper.sleep(DELAY_BETWEEN_REPOS_MILLIS);
      }
      firstRepository = false;

      // Only the GitHub call is in the try: ctx.log() is a cancellation checkpoint, and the
      // exception it throws when the job is cancelled must not be caught and counted as a repo
      // error.
      boolean deletedRepository;
      try {
        deletedRepository = repositoryService.deleteRepositoryIfEmpty(course, repository.name());
      } catch (Exception e) {
        errors++;
        ctx.log(
            String.format(
                "ERROR: Failed to delete/check repository %s: %s",
                repository.name(), e.getMessage()));
        continue;
      }
      if (deletedRepository) {
        deleted++;
        ctx.log(String.format("Deleted empty repository: %s", repository.name()));
      } else {
        retained++;
        ctx.log(String.format("Repo %s not deleted; commits exist.", repository.name()));
      }
    }

    ctx.log(String.format("%d repos deleted", deleted));
    ctx.log(String.format("%d repos retained", retained));
    ctx.log(String.format("%d errors", errors));
    ctx.log("Done");
  }
}
