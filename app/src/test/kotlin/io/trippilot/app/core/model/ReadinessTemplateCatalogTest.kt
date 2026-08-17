package io.trippilot.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessTemplateCatalogTest {
    @Test
    fun `required packs contain no duplicate stable id or destination fact`() {
        TravelScope.entries.forEach { scope ->
            val required = ReadinessTemplateCatalog.requiredItems(scope)
            assertEquals(required.size, required.map { it.id }.toSet().size)
            assertTrue(required.all { !it.optional })
            assertTrue(required.all { it.title.isNotBlank() && it.hint.isNotBlank() })
            assertTrue(required.none { template ->
                val copy = "${template.title} ${template.hint}".lowercase()
                listOf("다낭", "도쿄", "서울", "비자 발급", "환율", "긴급 전화").any(copy::contains)
            })
        }
    }

    @Test
    fun `international required pack has safe document and electronics checks`() {
        val ids = ReadinessTemplateCatalog.requiredItems(TravelScope.INTERNATIONAL).map { it.id }.toSet()
        assertTrue(ChecklistTemplateId.PASSPORT_VALIDITY_CHECK in ids)
        assertTrue(ChecklistTemplateId.ENTRY_REQUIREMENTS_OFFICIAL_CHECK in ids)
        assertTrue(ChecklistTemplateId.ADAPTER_NEED_CHECK in ids)
        assertFalse(ChecklistTemplateId.POST_TRIP_RECEIPTS in ids)
    }

    @Test
    fun `optional post trip pack remains opt in`() {
        val optional = ReadinessTemplateCatalog.optionalItems(TravelScope.DOMESTIC)
        assertTrue(optional.any { it.group == ChecklistGroup.POST_TRIP })
        assertTrue(ReadinessTemplateCatalog.optionalItems(TravelScope.DOMESTIC, ChecklistGroup.POST_TRIP).all { it.optional })
        assertEquals(
            ChecklistGroup.DIRECT_ADD,
            ReadinessTemplateCatalog.displayMetadata(ChecklistType.PREPARATION, null, "내가 적은 항목").group,
        )
    }
}
