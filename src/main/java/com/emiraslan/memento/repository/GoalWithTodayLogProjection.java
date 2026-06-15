package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.entity.GoalLog;

public interface GoalWithTodayLogProjection {
    Goal getGoal();
    GoalLog getTodayGoalLog();
}
