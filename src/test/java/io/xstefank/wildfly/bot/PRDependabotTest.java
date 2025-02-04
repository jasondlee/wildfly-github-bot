package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.model.RuntimeConstants;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.PROJECT_PATTERN_REGEX;
import static io.xstefank.wildfly.bot.utils.TestConstants.TEST_REPO;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;

/**
 * Tests for the PRs created by Dependabot.
 */
@QuarkusTest
@GitHubAppTest
public class PRDependabotTest {

    private String wildflyConfigFile;
    private PullRequestJson pullRequestJson;

    @Test
    void testDependabotPR() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description("Very detailed description of this upgrade.")
                .build();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                });
    }

    @Test
    void testDependabotPREmptyBody() throws IOException {
        wildflyConfigFile = """
                wildfly:
                      format:
                        description:
                          regexes:
                            - pattern: "https://issues.redhat.com/browse/WFLY-\\\\d+"
                """;
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .userLogin(RuntimeConstants.DEPENDABOT)
                .title("Bump some version from x.y to x.y+1")
                .description(null)
                .build();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("WildFly Bot recognized this PR as dependabot dependency update. " +
                            "Please create a WFLY issue and add new comment containing this JIRA link please.");
                    GHRepository repo = mocks.repository(TEST_REPO);
                    Util.verifyFormatFailure(repo, pullRequestJson, "description, title");
                    Util.verifyFailedFormatComment(mocks, pullRequestJson, String.format("""
                            - Invalid description content

                            - %s""", String.format(RuntimeConstants.DEFAULT_TITLE_MESSAGE,
                            PROJECT_PATTERN_REGEX.formatted("WFLY"))));
                });
    }
}
