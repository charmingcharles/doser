package edu.iis.mto.testreactor.doser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iis.mto.testreactor.doser.infuser.Infuser;
import edu.iis.mto.testreactor.doser.infuser.InfuserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    Medicine med1 = Medicine.of("Diltiazem");
    Medicine med2 = Medicine.of("Abatacept");


    @BeforeEach
    void setUp(){
        medicineDoser = new MedicineDoser(infuser, dosageLog, clock);
    }

    @Test
    void enoughOfMedicineTest() {
        Dose dose = Dose.of(Capacity.of(10, CapacityUnit.MILILITER), Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(MedicinePackage.of(med1, Capacity.of(1000, CapacityUnit.MILILITER)));
        Receipe receipe = Receipe.of(med1, dose, 1);
        DosingResult ds = medicineDoser.dose(receipe);
        assertEquals(ds, DosingResult.SUCCESS);
    }

    @Test
    void notEnoughOfMedicineTest() {
        Dose dose = Dose.of(Capacity.of(10, CapacityUnit.MILILITER), Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(MedicinePackage.of(med1, Capacity.of(5, CapacityUnit.MILILITER)));
        Receipe receipe = Receipe.of(med1, dose, 1);
        InsufficientMedicineException exception = assertThrows(InsufficientMedicineException.class, () -> medicineDoser.dose(receipe));
        assertEquals(exception.getMedicine(), med1);
    }

    @Test
    void notThisMedicineTest() {
        Dose dose = Dose.of(Capacity.of(10, CapacityUnit.MILILITER), Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(MedicinePackage.of(med2, Capacity.of(5, CapacityUnit.MILILITER)));
        Receipe receipe = Receipe.of(med1, dose, 1);
        UnavailableMedicineException exception = assertThrows(UnavailableMedicineException.class, () -> medicineDoser.dose(receipe));
        assertEquals(exception.getMedicine(), med1);
    }

    @Test
    void dispenseExceptionTest() throws InfuserException {
        MedicinePackage medicinePackage = MedicinePackage.of(med2, Capacity.of(500, CapacityUnit.MILILITER));
        Capacity capacity = Capacity.of(10, CapacityUnit.MILILITER);
        Dose dose = Dose.of(capacity, Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(medicinePackage);
        Mockito.doThrow(new InfuserException()).when(infuser).dispense(medicinePackage, capacity);
        Receipe receipe = Receipe.of(med2, dose, 1);
        DosingResult ds = medicineDoser.dose(receipe);
        assertEquals(DosingResult.SUCCESS, ds);
    }

    @Test
    void dispenseExceptionBehaviourTest() throws InfuserException {
        MedicinePackage medicinePackage = MedicinePackage.of(med2, Capacity.of(500, CapacityUnit.MILILITER));
        Capacity capacity = Capacity.of(10, CapacityUnit.MILILITER);
        Dose dose = Dose.of(capacity, Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(medicinePackage);
        Mockito.doThrow(new InfuserException("Example message")).when(infuser).dispense(medicinePackage, capacity);
        Receipe receipe = Receipe.of(med2, dose, 1);
        medicineDoser.dose(receipe);
        InOrder inOrder = Mockito.inOrder(dosageLog, infuser);
        inOrder.verify(dosageLog).logStartDose(med2, dose);
        inOrder.verify(infuser).dispense(medicinePackage, capacity);
        inOrder.verify(dosageLog).logDifuserError(dose, "Example message");
    }

    @Test
    void enoughOfMedicineInOrderTest() throws InfuserException {
        MedicinePackage medicinePackage = MedicinePackage.of(med1, Capacity.of(500, CapacityUnit.MILILITER));
        Capacity capacity = Capacity.of(10, CapacityUnit.MILILITER);
        Dose dose = Dose.of(capacity, Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(medicinePackage);
        Receipe receipe = Receipe.of(med1, dose, 1);
        medicineDoser.dose(receipe);
        InOrder inOrder = Mockito.inOrder(dosageLog, infuser, clock);
        inOrder.verify(dosageLog).logStart();
        inOrder.verify(dosageLog).logStartDose(med1, dose);
        inOrder.verify(infuser).dispense(medicinePackage, capacity);
        inOrder.verify(dosageLog).logEndDose(med1, dose);
        inOrder.verify(clock).wait(receipe.getDose().getPeriod());
    }

    @Test
    void enoughOfMedicineFewNumbersInOrderTest() throws InfuserException {
        MedicinePackage medicinePackage = MedicinePackage.of(med1, Capacity.of(500, CapacityUnit.MILILITER));
        Capacity capacity = Capacity.of(10, CapacityUnit.MILILITER);
        Dose dose = Dose.of(capacity, Period.of(12, TimeUnit.HOURS));
        medicineDoser.add(medicinePackage);
        Receipe receipe = Receipe.of(med1, dose, 4);
        medicineDoser.dose(receipe);
        InOrder inOrder = Mockito.inOrder(dosageLog, infuser, clock);
        inOrder.verify(dosageLog, Mockito.times(1)).logStart();
        inOrder.verify(dosageLog, Mockito.times(4)).logStartDose(med1, dose);
        inOrder.verify(infuser, Mockito.times(4)).dispense(medicinePackage, capacity);
        inOrder.verify(dosageLog, Mockito.times(4)).logEndDose(med1, dose);
        inOrder.verify(clock, Mockito.times(4)).wait(receipe.getDose().getPeriod());
    }

}
