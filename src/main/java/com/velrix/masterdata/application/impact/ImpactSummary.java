package com.velrix.masterdata.application.impact;

import java.util.List;

/**
 * 引用影响汇总（需求 §6.1 的响应结构）。
 *
 * @param deletable 没有任何 count &gt; 0 的引用项时为 true
 * @param message   中文摘要，DELETE 409 时直接作为提示文案
 * @param items     引用明细（count 为 0 的项不出现在这里）
 */
public record ImpactSummary(boolean deletable, String message, List<ImpactItem> items) {
}
