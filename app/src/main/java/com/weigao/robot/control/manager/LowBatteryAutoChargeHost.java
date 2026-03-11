package com.weigao.robot.control.manager;

/**
 * 需要在低电量回充流程接管前安全退出当前导航页的页面契约。
 */
public interface LowBatteryAutoChargeHost {

    /**
     * 停止当前导航并退出页面，把前台交还给底层页面，由低电量管理器继续弹出确认框。
     */
    void handoffToLowBatteryAutoCharge();
}
