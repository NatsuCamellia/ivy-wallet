package ivy.automate.issue

import arrow.core.Either
import arrow.core.raise.either
import ivy.automate.base.Constants
import ivy.automate.base.github.GitHubIssueArgs
import ivy.automate.base.github.GitHubService
import ivy.automate.base.github.model.GitHubIssueNumber
import ivy.automate.base.github.model.GitHubUser

sealed interface Action {
    val issueNumber: GitHubIssueNumber

    data class DoNothing(
        override val issueNumber: GitHubIssueNumber
    ) : Action

    data class AssignIssue(
        override val issueNumber: GitHubIssueNumber,
        val user: GitHubUser
    ) : Action

    data class NotApproved(
        val user: GitHubUser,
        override val issueNumber: GitHubIssueNumber
    ) : Action

    data class AlreadyTaken(
        val user: GitHubUser,
        override val issueNumber: GitHubIssueNumber,
        val assignee: GitHubUser,
    ) : Action
}

suspend fun GitHubService.determineAction(args: GitHubIssueArgs): Either<String, Action> = either {
    val issueNumber = args.issueNumber
    val intention = checkCommentsForIntention(issueNumber).bind()
        ?: return@either Action.DoNothing(issueNumber)

    when (intention) {
        is CommentIntention.TakeIssue -> toAction(intention, issueNumber).bind()
        CommentIntention.Unknown -> Action.DoNothing(issueNumber)
    }
}

private suspend fun GitHubService.toAction(
    intention: CommentIntention.TakeIssue,
    issueNumber: GitHubIssueNumber,
): Either<String, Action> = either {
    val assignee = checkIfIssueIsAssigned(issueNumber).bind()
    if (assignee != null) {
        return@either Action.AlreadyTaken(intention.user, issueNumber, assignee)
    }

    val approved = checkLabelsForApproved(issueNumber).bind()
    if (!approved) {
        return@either Action.NotApproved(intention.user, issueNumber)
    }

    Action.AssignIssue(issueNumber, intention.user)
}

private suspend fun GitHubService.checkCommentsForIntention(
    issueNumber: GitHubIssueNumber
): Either<String, CommentIntention?> = either {
    val comments = fetchIssueComments(issueNumber)
        .mapLeft { "Failed to fetch comments: $it." }
        .bind()

    val lastComment = comments.lastOrNull() ?: return@either null
    if (lastComment.author.username.value == Constants.IVY_BOT_USERNAME) {
        // Do nothing for Ivy BOT comments
        return@either null
    }

    analyzeCommentIntention(lastComment)
}

private suspend fun GitHubService.checkIfIssueIsAssigned(
    issueNumber: GitHubIssueNumber
): Either<String, GitHubUser?> = either {
    val issueInfo = fetchIssue(issueNumber)
        .mapLeft { "Failed to fetch issue: $it." }
        .bind()

    issueInfo.assignee
}

private suspend fun GitHubService.checkLabelsForApproved(
    issueNumber: GitHubIssueNumber
): Either<String, Boolean> = either {
    val labels = fetchIssueLabels(issueNumber)
        .mapLeft { "Failed to fetch labels: $it." }
        .bind()

    val isApproved = labels.any { it.name.value == "approved" }
    isApproved
}
