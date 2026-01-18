package com.weigao.robot.control.model;

/**
 * 舱门类型枚举
 * <p>
 * 对齐SDK {@code GatingType}。
 * </p>
 */
public enum DoorType {

    /** 四舱 */
    FOUR(0, 101),

    /** 双舱 */
    DOUBLE(1, 102),

    /** T字型舱 */
    THREE(2, 103),

    /** 倒T型舱 */
    THREE_REVERSE(3, 104),

    /** 自动识别 */
    AUTO(-1, 105);

    /** 舱门类型ID（SDK GatingType） */
    private final int typeId;

    /** 设置类型常量（SDK SET_TYPE_XXX） */
    private final int setTypeCode;

    DoorType(int typeId, int setTypeCode) {
        this.typeId = typeId;
        this.setTypeCode = setTypeCode;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getSetTypeCode() {
        return setTypeCode;
    }

    /**
     * 根据类型ID获取枚举值
     *
     * @param typeId 类型ID
     * @return 对应的枚举值，未找到返回null
     */
    public static DoorType fromTypeId(int typeId) {
        for (DoorType type : values()) {
            if (type.typeId == typeId) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据设置类型码获取枚举值
     *
     * @param setTypeCode 设置类型码
     * @return 对应的枚举值，未找到返回null
     */
    public static DoorType fromSetTypeCode(int setTypeCode) {
        for (DoorType type : values()) {
            if (type.setTypeCode == setTypeCode) {
                return type;
            }
        }
        return null;
    }
}
