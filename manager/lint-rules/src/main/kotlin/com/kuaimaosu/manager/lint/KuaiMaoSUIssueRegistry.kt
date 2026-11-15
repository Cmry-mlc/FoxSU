package com.kuaimaosu.manager.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API

class KuaiMaoSUIssueRegistry : IssueRegistry() {
    override val issues = listOf(
        SegmentedColumnScopeConditionDetector.ISSUE,
        DirectHorizontalPagerDetector.ISSUE,
    )

    override val api = CURRENT_API
}
