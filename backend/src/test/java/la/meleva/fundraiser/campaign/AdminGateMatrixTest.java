package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.compliance.GateType;
import la.meleva.fundraiser.shared.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdminGateMatrixTest {

    @Test
    void demoRequiresNoGates() {
        Set<GateType> gates = AdminGateMatrix.requiredGates(CampaignMode.DEMO);
        assertTrue(gates.isEmpty());
    }

    @Test
    void commercialPromotionRequiresFourGates() {
        Set<GateType> gates = AdminGateMatrix.requiredGates(CampaignMode.COMMERCIAL_PROMOTION);
        assertEquals(4, gates.size());
        assertTrue(gates.contains(GateType.G_LEGAL));
        assertTrue(gates.contains(GateType.G_SECURITY));
        assertTrue(gates.contains(GateType.G_OPERATIONS));
        assertTrue(gates.contains(GateType.G_LAUNCH));
    }

    @Test
    void philanthropicRequiresAllGates() {
        Set<GateType> gates = AdminGateMatrix.requiredGates(CampaignMode.PHILANTHROPIC_AUTHORIZED);
        assertEquals(GateType.values().length, gates.size());
    }

    @Test
    void demoSkipsGateCheck() {
        assertDoesNotThrow(() ->
                AdminGateMatrix.requireGatesForTransition(CampaignMode.DEMO, CampaignStatus.OPEN));
    }

    @Test
    void nonDemoBlocksOpenWithoutGates() {
        var ex = assertThrows(ErrorCode.BusinessException.class, () ->
                AdminGateMatrix.requireGatesForTransition(
                        CampaignMode.COMMERCIAL_PROMOTION, CampaignStatus.OPEN));
        assertEquals(ErrorCode.CAMPAIGN_GATES_INCOMPLETE, ex.errorCode());
    }
}
