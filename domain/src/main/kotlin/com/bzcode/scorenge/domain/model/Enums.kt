package com.bzcode.scorenge.domain.model

/** Whether each player or each team is scored. */
enum class ScoringMode { INDIVIDUAL, TEAM }

/** Which end of the score range wins. */
enum class ScoringDirection { HIGHER_WINS, LOWER_WINS }

/**
 * How a match is scored, chosen on the New match screen.
 * FINISHING_ORDER: each round the players or teams are tapped in the order they finished, and each
 * place earns points (see [com.bzcode.scorenge.domain.match.FinishingOrder]); highest total wins.
 */
enum class MatchScoring { HIGHEST_WINS, LOWEST_WINS, FINISHING_ORDER }

/** How a match's round results are entered: typed scores, or a tapped finishing order. */
enum class ScoreEntry { POINTS, FINISHING_ORDER }

/** Fixed: the round count is set before the match starts. Dynamic: rounds are added until the user finishes. */
enum class RoundMode { FIXED, DYNAMIC }

/** Lifecycle of a match (called "event" in the data model). Completed matches can never be reopened. */
enum class EventStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

/**
 * PENDING: created when the previous round completes; the clock has not started.
 * ACTIVE: opened by the user; scores can be entered.
 * COMPLETED: locked.
 */
enum class RoundStatus { PENDING, ACTIVE, COMPLETED }

/** The entity that receives a score: one player (individual mode) or one team (team mode). */
enum class ScoringSubjectType { PLAYER, TEAM }
