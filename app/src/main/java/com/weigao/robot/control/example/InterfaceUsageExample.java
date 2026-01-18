package com.weigao.robot.control.example;

import android.util.Log;

import com.weigao.robot.control.callback.ApiError;
import com.weigao.robot.control.callback.ErrorCode;
import com.weigao.robot.control.callback.IResultCallback;
import com.weigao.robot.control.callback.IStateCallback;
import com.weigao.robot.control.model.AudioConfig;
import com.weigao.robot.control.model.DeliveryConfig;
import com.weigao.robot.control.model.DeliveryTask;
import com.weigao.robot.control.model.PointInfo;
import com.weigao.robot.control.model.RobotState;
import com.weigao.robot.control.service.IAudioService;
import com.weigao.robot.control.service.IDeliveryService;
import com.weigao.robot.control.service.IDoorService;
import com.weigao.robot.control.service.IPointService;
import com.weigao.robot.control.service.IRemoteService;

import java.util.Arrays;
import java.util.List;

public class InterfaceUsageExample {
    private static final String TAG = "InterfaceExample";

    private IDeliveryService deliveryService;
    private IPointService pointService;
    private IDoorService doorService;
    private IRemoteService remoteService;
    private IAudioService audioService;

    public InterfaceUsageExample(IDeliveryService deliveryService,
                                  IPointService pointService,
                                  IDoorService doorService,
                                  IRemoteService remoteService,
                                  IAudioService audioService) {
        this.deliveryService = deliveryService;
        this.pointService = pointService;
        this.doorService = doorService;
        this.remoteService = remoteService;
        this.audioService = audioService;
    }

    public void exampleStandardDelivery() {
        Log.d(TAG, "=== 标准配送示例 ===");

        pointService.getAllPoints(new IResultCallback<List<PointInfo>>() {
            @Override
            public void onSuccess(List<PointInfo> points) {
                if (points.size() >= 2) {
                    PointInfo startPoint = points.get(0);
                    PointInfo endPoint = points.get(1);

                    DeliveryTask task = new DeliveryTask();
                    task.setType(DeliveryTask.DeliveryType.STANDARD);
                    task.setPoints(Arrays.asList(startPoint, endPoint));

                    DeliveryConfig config = new DeliveryConfig();
                    config.setDeliverySpeed(50);
                    config.setReturnSpeed(60);
                    config.setStayDuration(30);
                    task.setConfig(config);

                    deliveryService.startStandardDelivery(task, new IResultCallback<String>() {
                        @Override
                        public void onSuccess(String taskId) {
                            Log.d(TAG, "标准配送任务已启动，任务ID: " + taskId);

                            pollDeliveryStatus(taskId);
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "启动标准配送失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "点位数量不足，至少需要2个点位");
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取点位列表失败: " + error.getMessage());
            }
        });
    }

    public void exampleLoopDelivery() {
        Log.d(TAG, "=== 循环配送示例 ===");

        pointService.getPointsByFloor(1, new IResultCallback<List<PointInfo>>() {
            @Override
            public void onSuccess(List<PointInfo> points) {
                if (points.size() >= 3) {
                    DeliveryTask task = new DeliveryTask();
                    task.setType(DeliveryTask.DeliveryType.LOOP);
                    task.setPoints(Arrays.asList(points.get(0), points.get(1), points.get(2)));

                    DeliveryConfig config = new DeliveryConfig();
                    config.setDeliverySpeed(40);
                    config.setLoopCount(5);
                    config.setLoopDuration(300);
                    task.setConfig(config);

                    deliveryService.startLoopDelivery(task, new IResultCallback<String>() {
                        @Override
                        public void onSuccess(String taskId) {
                            Log.d(TAG, "循环配送任务已启动，任务ID: " + taskId);

                            pollDeliveryStatus(taskId);
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "启动循环配送失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "点位数量不足，至少需要3个点位");
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取楼层点位失败: " + error.getMessage());
            }
        });
    }

    public void exampleRecoveryDelivery() {
        Log.d(TAG, "=== 回收配送示例 ===");

        pointService.getAllPoints(new IResultCallback<List<PointInfo>>() {
            @Override
            public void onSuccess(List<PointInfo> points) {
                if (points.size() >= 3) {
                    PointInfo recoveryPoint = points.get(points.size() - 1);

                    DeliveryTask task = new DeliveryTask();
                    task.setType(DeliveryTask.DeliveryType.RECOVERY);
                    task.setPoints(points.subList(0, points.size() - 1));

                    deliveryService.startRecoveryDelivery(task, new IResultCallback<String>() {
                        @Override
                        public void onSuccess(String taskId) {
                            Log.d(TAG, "回收配送任务已启动，任务ID: " + taskId);

                            pollDeliveryStatus(taskId);
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "启动回收配送失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "点位数量不足，至少需要3个点位");
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取点位列表失败: " + error.getMessage());
            }
        });
    }

    public void exampleSlowDelivery() {
        Log.d(TAG, "=== 慢速配送示例 ===");

        pointService.getRecentPoints(new IResultCallback<List<PointInfo>>() {
            @Override
            public void onSuccess(List<PointInfo> points) {
                if (!points.isEmpty()) {
                    PointInfo targetPoint = points.get(0);

                    DeliveryTask task = new DeliveryTask();
                    task.setType(DeliveryTask.DeliveryType.SLOW);
                    task.setPoints(Arrays.asList(targetPoint));

                    DeliveryConfig config = new DeliveryConfig();
                    config.setDeliverySpeed(30);
                    config.setSlowStart(true);
                    task.setConfig(config);

                    deliveryService.startSlowDelivery(task, new IResultCallback<String>() {
                        @Override
                        public void onSuccess(String taskId) {
                            Log.d(TAG, "慢速配送任务已启动，任务ID: " + taskId);

                            pollDeliveryStatus(taskId);
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "启动慢速配送失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "没有最近使用的点位");
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取最近点位失败: " + error.getMessage());
            }
        });
    }

    private void pollDeliveryStatus(String taskId) {
        final int[] pollCount = {0};
        final int maxPollCount = 60;

        new Thread(() -> {
            while (pollCount[0] < maxPollCount) {
                try {
                    Thread.sleep(2000);
                    pollCount[0]++;

                    final int currentPoll = pollCount[0];
                    deliveryService.getDeliveryStatus(taskId, new IResultCallback<DeliveryTask>() {
                        @Override
                        public void onSuccess(DeliveryTask task) {
                            Log.d(TAG, "任务状态: " + task.getStatus() + ", 进度: " + task.getProgress() + "%");

                            if (task.getStatus() == DeliveryTask.DeliveryStatus.COMPLETED ||
                                task.getStatus() == DeliveryTask.DeliveryStatus.CANCELLED ||
                                task.getStatus() == DeliveryTask.DeliveryStatus.FAILED) {
                                pollCount[0] = maxPollCount;
                            }
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "获取任务状态失败: " + error.getMessage());
                            pollCount[0] = maxPollCount;
                        }
                    });

                } catch (InterruptedException e) {
                    Log.e(TAG, "轮询被中断", e);
                    break;
                }
            }
        }).start();
    }

    public void examplePauseAndResumeDelivery(String taskId) {
        Log.d(TAG, "=== 暂停和恢复配送示例 ===");

        deliveryService.pauseDelivery(taskId, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "配送任务已暂停");

                try {
                    Thread.sleep(5000);

                    deliveryService.resumeDelivery(taskId, new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "配送任务已恢复");
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "恢复配送失败: " + error.getMessage());
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "等待被中断", e);
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "暂停配送失败: " + error.getMessage());
            }
        });
    }

    public void exampleCancelDelivery(String taskId) {
        Log.d(TAG, "=== 取消配送示例 ===");

        deliveryService.cancelDelivery(taskId, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "配送任务已取消");
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "取消配送失败: " + error.getMessage());
            }
        });
    }

    public void examplePointManagement() {
        Log.d(TAG, "=== 点位管理示例 ===");

        PointInfo newPoint = new PointInfo();
        newPoint.setName("新点位");
        newPoint.setFloor(2);
        newPoint.setX(15.5);
        newPoint.setY(25.3);
        newPoint.setType("DESTINATION");

        pointService.addPoint(newPoint, new IResultCallback<String>() {
            @Override
            public void onSuccess(String pointId) {
                Log.d(TAG, "点位添加成功，ID: " + pointId);

                newPoint.setId(pointId);
                newPoint.setName("更新后的点位名称");

                pointService.updatePoint(newPoint, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "点位更新成功");

                        pointService.setReturnPoint(pointId, new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "返回点设置成功");
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "设置返回点失败: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "更新点位失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "添加点位失败: " + error.getMessage());
            }
        });
    }

    public void exampleDoorControl() {
        Log.d(TAG, "=== 舱门控制示例 ===");

        doorService.verifyPassword(1, "123456", new IResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean verified) {
                if (verified) {
                    Log.d(TAG, "密码验证通过");

                    doorService.openDoor(1, new IResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "舱门1已打开");

                            try {
                                Thread.sleep(10000);

                                doorService.closeDoor(1, new IResultCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        Log.d(TAG, "舱门1已关闭");
                                    }

                                    @Override
                                    public void onError(ApiError error) {
                                        Log.e(TAG, "关闭舱门失败: " + error.getMessage());
                                    }
                                });
                            } catch (InterruptedException e) {
                                Log.e(TAG, "等待被中断", e);
                            }
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "打开舱门失败: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "密码验证失败");
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "密码验证出错: " + error.getMessage());
            }
        });
    }

    public void exampleDoorSettings() {
        Log.d(TAG, "=== 舱门设置示例 ===");

        doorService.setFootSwitchEnabled(true, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "脚踩灯光开关门已启用");

                doorService.setAutoLeaveEnabled(true, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "到达后自动离开已启用");
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "设置自动离开失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "设置脚踩灯光失败: " + error.getMessage());
            }
        });
    }

    public void exampleRemoteCall() {
        Log.d(TAG, "=== 远程呼叫示例 ===");

        remoteService.enableRemoteCall(true, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "远程呼叫已启用");

                pointService.getRecentPoints(new IResultCallback<List<PointInfo>>() {
                    @Override
                    public void onSuccess(List<PointInfo> points) {
                        if (!points.isEmpty()) {
                            PointInfo targetPoint = points.get(0);

                            remoteService.callArrival(targetPoint, 30, new IResultCallback<String>() {
                                @Override
                                public void onSuccess(String taskId) {
                                    Log.d(TAG, "呼叫到达任务已启动，任务ID: " + taskId);
                                }

                                @Override
                                public void onError(ApiError error) {
                                    Log.e(TAG, "呼叫到达失败: " + error.getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "获取最近点位失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "启用远程呼叫失败: " + error.getMessage());
            }
        });
    }

    public void exampleRemoteLoopCall() {
        Log.d(TAG, "=== 远程循环呼叫示例 ===");

        pointService.getPointsByFloor(1, new IResultCallback<List<PointInfo>>() {
            @Override
            public void onSuccess(List<PointInfo> points) {
                if (!points.isEmpty()) {
                    PointInfo targetPoint = points.get(0);

                    remoteService.callLoop(targetPoint, 60, new IResultCallback<String>() {
                        @Override
                        public void onSuccess(String taskId) {
                            Log.d(TAG, "呼叫循环任务已启动，任务ID: " + taskId);
                        }

                        @Override
                        public void onError(ApiError error) {
                            Log.e(TAG, "呼叫循环失败: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "获取楼层点位失败: " + error.getMessage());
            }
        });
    }

    public void exampleRemoteRecoveryCall() {
        Log.d(TAG, "=== 远程回收呼叫示例 ===");

        remoteService.callRecovery(new IResultCallback<String>() {
            @Override
            public void onSuccess(String taskId) {
                Log.d(TAG, "呼叫回收任务已启动，任务ID: " + taskId);
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "呼叫回收失败: " + error.getMessage());
            }
        });
    }

    public void exampleStayDuration() {
        Log.d(TAG, "=== 停留时长设置示例 ===");

        remoteService.setStayDuration(60, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "停留时长已设置为60秒");

                remoteService.getStayDuration(new IResultCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer duration) {
                        Log.d(TAG, "当前停留时长: " + duration + "秒");
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "获取停留时长失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "设置停留时长失败: " + error.getMessage());
            }
        });
    }

    public void exampleAudioControl() {
        Log.d(TAG, "=== 音频控制示例 ===");

        audioService.setVoiceVolume(80, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "语音音量已设置为80");

                audioService.setDeliveryVolume(70, new IResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "配送音量已设置为70");

                        audioService.setAnnouncementFrequency(30, new IResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "语音播报频率已设置为30秒");
                            }

                            @Override
                            public void onError(ApiError error) {
                                Log.e(TAG, "设置播报频率失败: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "设置配送音量失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "设置语音音量失败: " + error.getMessage());
            }
        });
    }

    public void exampleAudioConfig() {
        Log.d(TAG, "=== 音频配置示例 ===");

        AudioConfig config = new AudioConfig();
        config.setVoiceVolume(85);
        config.setDeliveryVolume(75);
        config.setAnnouncementFrequency(25);
        config.setDeliveryMusicPath("/sdcard/music/delivery.mp3");
        config.setLoopMusicPath("/sdcard/music/loop.mp3");

        audioService.updateAudioConfig(config, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "音频配置已更新");

                audioService.getAudioConfig(new IResultCallback<AudioConfig>() {
                    @Override
                    public void onSuccess(AudioConfig config) {
                        Log.d(TAG, "语音音量: " + config.getVoiceVolume());
                        Log.d(TAG, "配送音量: " + config.getDeliveryVolume());
                        Log.d(TAG, "播报频率: " + config.getAnnouncementFrequency() + "秒");
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "获取音频配置失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "更新音频配置失败: " + error.getMessage());
            }
        });
    }

    public void exampleDeliveryConfig() {
        Log.d(TAG, "=== 配送配置示例 ===");

        DeliveryConfig config = new DeliveryConfig();
        config.setShowRecentPoints(true);
        config.setDeliveryMode(DeliveryConfig.DeliveryMode.SINGLE_FLOOR_MULTI_POINT);
        config.setStayDuration(30);
        config.setPauseDuration(5);
        config.setDeliverySpeed(50);
        config.setReturnSpeed(60);
        config.setSlowStart(true);

        deliveryService.updateDeliveryConfig(config, new IResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "配送配置已更新");

                deliveryService.getDeliveryConfig(new IResultCallback<DeliveryConfig>() {
                    @Override
                    public void onSuccess(DeliveryConfig config) {
                        Log.d(TAG, "显示最近点位: " + config.isShowRecentPoints());
                        Log.d(TAG, "配送速度: " + config.getDeliverySpeed() + " cm/s");
                        Log.d(TAG, "返回速度: " + config.getReturnSpeed() + " cm/s");
                        Log.d(TAG, "停留时长: " + config.getStayDuration() + " 秒");
                    }

                    @Override
                    public void onError(ApiError error) {
                        Log.e(TAG, "获取配送配置失败: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                Log.e(TAG, "更新配送配置失败: " + error.getMessage());
            }
        });
    }

    public void exampleStateListener(IStateCallback stateCallback) {
        Log.d(TAG, "=== 状态监听示例 ===");

        stateCallback.onStateChanged(new RobotState());
        stateCallback.onLocationChanged(10.5, 20.3);
        stateCallback.onBatteryLevelChanged(85);
        stateCallback.onScramButtonPressed(false);
    }

    public void exampleErrorHandling() {
        Log.d(TAG, "=== 错误处理示例 ===");

        ApiError networkError = new ApiError(
            ErrorCode.ERROR_NETWORK,
            "网络连接失败",
            ApiError.ErrorType.NETWORK_ERROR
        );

        Log.e(TAG, "错误码: " + networkError.getCode());
        Log.e(TAG, "错误消息: " + networkError.getMessage());
        Log.e(TAG, "错误类型: " + networkError.getType());

        ApiError validationError = new ApiError(
            ErrorCode.ERROR_VALIDATION,
            "参数验证失败",
            ApiError.ErrorType.VALIDATION_ERROR
        );

        Log.e(TAG, "错误码: " + validationError.getCode());
        Log.e(TAG, "错误消息: " + validationError.getMessage());
        Log.e(TAG, "错误类型: " + validationError.getType());

        String errorMessage = ErrorCode.getErrorMessage(ErrorCode.ERROR_NETWORK);
        Log.e(TAG, "错误消息: " + errorMessage);
    }
}
