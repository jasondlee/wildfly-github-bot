package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.wildfly.bot.utils.MockedContext;
import io.xstefank.wildfly.bot.utils.PullRequestJson;
import io.xstefank.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_COMMIT_MESSAGE;
import static io.xstefank.wildfly.bot.utils.TestConstants.INVALID_TITLE;
import static io.xstefank.wildfly.bot.utils.TestConstants.VALID_PR_TEMPLATE_JSON;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest
public class PRSkipPullRequestTest {

    private static PullRequestJson pullRequestJson;
    private MockedContext mockedContext;
    private static final String wildflyConfigFile = """
            wildfly:
              format:
                title:
                  enabled: true
                commit:
                  enabled: true
            """;

    @Test
    void testSkippingFormatCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .description("""
                        Hi


                        line start @wildfly-bot[bot] skip format random things

                        to pass on my local env @wildfly-github-bot-fork[bot] skip format thanks

                        finished""")
                .build();
        mockedContext = MockedContext.builder(pullRequestJson.id()).commit(INVALID_COMMIT_MESSAGE);
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    // Following invocations are used for logging
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getNumber();
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                });
    }

    @Test
    void testSkippingFormatCheckOnDraft() throws IOException {
        pullRequestJson = PullRequestJson.builder(VALID_PR_TEMPLATE_JSON)
                .title(INVALID_TITLE)
                .build();
        mockedContext = MockedContext.builder(pullRequestJson.id()).draft();
        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson, mockedContext))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).getBody();
                    verify(mocks.pullRequest(pullRequestJson.id())).listFiles();
                    verify(mocks.pullRequest(pullRequestJson.id()), times(2)).isDraft();
                    verifyNoMoreInteractions(mocks.pullRequest(pullRequestJson.id()));
                });
    }
}
