package com.weigao.robot.control.core.state;

import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.callback.IStateCallback;

public interface IStateManager {
    RobotState getCurrentState();

    void updateState(RobotState state);

    void registerStateListener(IStateCallback listener);

    void unregisterStateListener(IStateCallback listener);

    void clearStateListeners();

    void saveState();

    void loadState();
}
