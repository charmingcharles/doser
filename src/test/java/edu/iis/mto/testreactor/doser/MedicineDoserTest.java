package edu.iis.mto.testreactor.doser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.iis.mto.testreactor.doser.infuser.Infuser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class MedicineDoserTest {

    MedicineDoser medicineDoser;

    @Mock
    Infuser infuser;

    @Mock
    DosageLog dosageLog;

    @Mock
    Clock clock;


    @BeforeEach
    void setUp(){
        medicineDoser = new MedicineDoser(infuser, dosageLog, clock);
    }

    @Test
    void enoughOfMedicineTest() {
        Medicine medicine = Medicine.of("Diltiazem");
        Dose dose = Dose.of(Capacity.of(10, CapacityUnit.MILILITER), Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(MedicinePackage.of(medicine, Capacity.of(1000, CapacityUnit.MILILITER)));
        Receipe receipe = Receipe.of(medicine, dose, 1);
        DosingResult ds = medicineDoser.dose(receipe);
        assertEquals(ds, DosingResult.SUCCESS);
    }
}
