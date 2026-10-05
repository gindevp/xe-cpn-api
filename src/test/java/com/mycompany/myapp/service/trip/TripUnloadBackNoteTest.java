package com.mycompany.myapp.service.trip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TripUnloadBackNoteTest {

    @Test
    void destWarehouseInOnlyWhenSeqsPresent() {
        assertThat(TripFacadeService.hasDestWarehouseIn(null)).isFalse();
        assertThat(TripFacadeService.hasDestWarehouseIn("[KIEN]A[/KIEN]\n[WHIN][/WHIN]")).isFalse();
        assertThat(TripFacadeService.hasDestWarehouseIn("[WHOUT]1,2[/WHOUT]")).isFalse();
        assertThat(TripFacadeService.hasDestWarehouseIn("[WHIN]1[/WHIN]\nghi chú")).isTrue();
    }

    @Test
    void stripWarehouseOutKeepsOtherMarkers() {
        String note = "[KIEN]A,B[/KIEN]\n[WHOUT]1,2[/WHOUT]\n[DRVSIGN t=1]data[/DRVSIGN]\nghi chú";
        assertThat(TripFacadeService.stripWarehouseOut(note)).isEqualTo("[KIEN]A,B[/KIEN]\n[DRVSIGN t=1]data[/DRVSIGN]\nghi chú");
        assertThat(TripFacadeService.stripWarehouseOut("[WHOUT]1[/WHOUT]")).isNull();
        assertThat(TripFacadeService.stripWarehouseOut(null)).isNull();
    }

    @Test
    void withAllWarehouseInMarksEveryPackage() {
        assertThat(TripFacadeService.withAllWarehouseIn(null, null)).isEqualTo("[WHIN]1[/WHIN]");
        assertThat(TripFacadeService.withAllWarehouseIn("[WHOUT]1,2,3[/WHOUT]\nghi chú", 3)).isEqualTo(
            "[WHOUT]1,2,3[/WHOUT]\nghi chú\n[WHIN]1,2,3[/WHIN]"
        );
        assertThat(TripFacadeService.withAllWarehouseIn("[LOAI]A[/LOAI]\n[WHIN]2[/WHIN]\n[CUOC]1[/CUOC]", 2)).isEqualTo(
            "[LOAI]A[/LOAI]\n[CUOC]1[/CUOC]\n[WHIN]1,2[/WHIN]"
        );
        assertThat(TripFacadeService.hasDestWarehouseIn(TripFacadeService.withAllWarehouseIn("x", 1))).isTrue();
    }
}
