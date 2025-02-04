package io.xstefank.wildfly.bot;

import io.quarkiverse.githubapp.event.PullRequest;
import io.xstefank.wildfly.bot.util.GithubProcessor;
import io.xstefank.wildfly.bot.util.PullRequestLogger;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_FIX_ME;
import static io.xstefank.wildfly.bot.model.RuntimeConstants.LABEL_NEEDS_REBASE;

public class PullRequestLabelProcessor {

    private static final Logger LOG_DELEGATE = Logger.getLogger(PullRequestFormatProcessor.class);
    private final PullRequestLogger LOG = new PullRequestLogger(LOG_DELEGATE);

    @Inject
    GithubProcessor githubProcessor;

    void pullRequestLabelCheck(@PullRequest.Synchronize @PullRequest.Reopened GHEventPayload.PullRequest pullRequestPayload)
            throws IOException {
        GHPullRequest pullRequest = pullRequestPayload.getPullRequest();
        LOG.setPullRequest(pullRequest);
        githubProcessor.LOG.setPullRequest(pullRequest);

        String message = githubProcessor.skipPullRequest(pullRequest);
        if (message != null) {
            LOG.infof("Skipping labelling due to %s", message);
            return;
        }

        List<String> labelsToAdd = new ArrayList<>();
        List<String> labelsToRemove = new ArrayList<>(List.of(LABEL_FIX_ME));

        if (!pullRequest.getMergeable()) {
            labelsToAdd.add(LABEL_NEEDS_REBASE);
        } else {
            labelsToRemove.add(LABEL_NEEDS_REBASE);
        }

        githubProcessor.updateLabels(pullRequest, labelsToAdd, labelsToRemove);
    }
}
