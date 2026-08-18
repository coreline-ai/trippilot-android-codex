package io.trippilot.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    @Test
    fun `gap adoption templates are general and window mapped`() {
        val newRequired = ReadinessTemplateCatalog.requiredItems(TravelScope.DOMESTIC).map { it.id }
        assertTrue(ChecklistTemplateId.EMERGENCY_CONTACT_COPY in newRequired)
        val optionalIds = ReadinessTemplateCatalog.optionalItems(TravelScope.DOMESTIC).map { it.id }
        listOf(
            ChecklistTemplateId.BACKUP_PAYMENT_METHOD,
            ChecklistTemplateId.OFFLINE_MAPS_READY,
            ChecklistTemplateId.LOCAL_TRANSIT_APP_READY,
            ChecklistTemplateId.WEATHER_PROOF_STORAGE,
            ChecklistTemplateId.RAIN_SUN_PROTECTION,
            ChecklistTemplateId.INSECT_PROTECTION,
        ).forEach { id -> assertTrue(id in optionalIds) }
        // Every post-trip template carries a window; pre-trip templates never do.
        ReadinessTemplateCatalog.postTripItems(PostTripWindow.WITHIN_48_HOURS).forEach {
            assertEquals(PostTripWindow.WITHIN_48_HOURS, it.postTripWindow)
        }
        ReadinessTemplateCatalog.requiredItems(TravelScope.INTERNATIONAL).forEach {
            assertNull(it.postTripWindow)
        }
        // Legacy post-trip templates were remapped, not duplicated.
        assertEquals(PostTripWindow.WITHIN_ONE_WEEK, ReadinessTemplateCatalog.find("POST_TRIP_RECEIPTS")?.postTripWindow)
        assertEquals(PostTripWindow.WITHIN_48_HOURS, ReadinessTemplateCatalog.find("POST_TRIP_RETURN_CHECK")?.postTripWindow)
    }

    @Test
    fun `problem response catalog has seven general categories without agency facts`() {
        assertEquals(7, ProblemResponseCatalog.all().size)
        assertEquals(7, ProblemResponseCatalog.all().map { it.id }.toSet().size)
        ProblemResponseCatalog.all().forEach { category ->
            assertTrue(category.steps.size >= 3)
            val copy = (category.steps.joinToString(" ") + ProblemResponseCatalog.NOT_EMERGENCY_NOTICE).lowercase()
            listOf("112", "119", "대사관", "병원 명", "보상이 확정", "신고 의무가").none(copy::contains)
        }
    }
}
