package com.hitachi.confirmnfc.enums

/**
 * Fragment遷移時の操作種別。
 */
enum class FragmentOpCmd {
    /** 既存表示を置き換える。 */
    OP_REPLACE,

    /** 既存Fragmentを隠して切り替える。 */
    OP_SWITCH,

    /** 既存Fragmentを破棄して移動する。 */
    OP_MOVE
}
