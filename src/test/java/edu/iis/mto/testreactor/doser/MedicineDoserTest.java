package edu.iis.mto.testreactor.doser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iis.mto.testreactor.doser.infuser.Infuser;
import edu.iis.mto.testreactor.doser.infuser.InfuserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

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

    Medicine pills = Medicine.of("Pills");
    Medicine syrup = Medicine.of("Syrup");

    Capacity capacity100 = Capacity.of(100, CapacityUnit.MILILITER);

    Period period12 = Period.of(12, TimeUnit.HOURS);
    Period period24 = Period.of(1, TimeUnit.DAYS);

    @BeforeEach
    void setUp(){
        medicineDoser = new MedicineDoser(infuser, dosageLog, clock);
    }

    @Test
    void onePillEvery12HoursTest(){
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 1);
        medicineDoser.add(MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER)));
        DosingResult result = medicineDoser.dose(pillsRecipe);
        assertEquals(DosingResult.SUCCESS, result);
    }

    @Test
    void verifyThreePillsEvery12HoursTest(){
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 3);
        medicineDoser.add(MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER)));
        DosingResult result = medicineDoser.dose(pillsRecipe);
        assertEquals(DosingResult.SUCCESS, result);
        Mockito.verify(clock, Mockito.times(3)).wait(period12);
    }

    @Test
    void twoPillsAndSyrupEvery24HoursTest(){
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period24), 2);
        Receipe syrupRecipe = Receipe.of(syrup, Dose.of(capacity100, period24), 1);
        medicineDoser.add(MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER)));
        medicineDoser.add(MedicinePackage.of(syrup, Capacity.of(1200, CapacityUnit.MILILITER)));
        DosingResult result1 = medicineDoser.dose(pillsRecipe);
        DosingResult result2 = medicineDoser.dose(syrupRecipe);
        assertEquals(DosingResult.SUCCESS, result1);
        assertEquals(DosingResult.SUCCESS, result2);
    }

    @Test
    void notEnoughPillsTest(){
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 1);
        medicineDoser.add(MedicinePackage.of(pills, Capacity.of(50, CapacityUnit.MILILITER)));
        MedicineException exception = Assertions.assertThrows(InsufficientMedicineException.class, () -> medicineDoser.dose(pillsRecipe));
        assertEquals(exception.getMedicine(), pills);
    }

    @Test
    void noSyrupTest(){
        Receipe pillsRecipe = Receipe.of(syrup, Dose.of(capacity100, period12), 2);
        medicineDoser.add(MedicinePackage.of(pills, Capacity.of(50, CapacityUnit.LITER)));
        MedicineException exception = Assertions.assertThrows(UnavailableMedicineException.class, () -> medicineDoser.dose(pillsRecipe));
        assertEquals(exception.getMedicine(), syrup);
    }

    @Test
    void correctOrderTest() throws InfuserException {
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 1);
        MedicinePackage medicinePackage = MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER));
        medicineDoser.add(medicinePackage);
        medicineDoser.dose(pillsRecipe);
        InOrder inOrder = Mockito.inOrder(infuser, dosageLog, clock);
        inOrder.verify(dosageLog).logStart();
        inOrder.verify(dosageLog).logStartDose(pills, Dose.of(capacity100, period12));
        inOrder.verify(infuser).dispense(medicinePackage, capacity100);
        inOrder.verify(dosageLog).logEndDose(pills, Dose.of(capacity100, period12));
        inOrder.verify(clock).wait(period12);
        inOrder.verify(dosageLog).logEnd();
    }

    @Test
    void timesRunTest() throws InfuserException {
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 1);
        MedicinePackage medicinePackage = MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER));
        medicineDoser.add(medicinePackage);
        medicineDoser.dose(pillsRecipe);
        Mockito.verify(dosageLog).logStart();
        Mockito.verify(dosageLog).logStartDose(pills, Dose.of(capacity100, period12));
        Mockito.verify(infuser).dispense(medicinePackage, capacity100);
        Mockito.verify(dosageLog).logEndDose(pills, Dose.of(capacity100, period12));
        Mockito.verify(clock).wait(period12);
        Mockito.verify(dosageLog).logEnd();
    }

    @Test
    void threeTimesRunTest() throws InfuserException {
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 3);
        MedicinePackage medicinePackage = MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER));
        medicineDoser.add(medicinePackage);
        medicineDoser.dose(pillsRecipe);
        Mockito.verify(dosageLog, Mockito.times(1)).logStart();
        Mockito.verify(dosageLog, Mockito.times(3)).logStartDose(pills, Dose.of(capacity100, period12));
        Mockito.verify(infuser, Mockito.times(3)).dispense(medicinePackage, capacity100);
        Mockito.verify(dosageLog, Mockito.times(3)).logEndDose(pills, Dose.of(capacity100, period12));
        Mockito.verify(clock, Mockito.times(3)).wait(period12);
        Mockito.verify(dosageLog, Mockito.times(1)).logEnd();
    }

    @Test
    void twoPillsAndSyrupEvery24HoursVerifyTest() throws InfuserException {
        Dose dose24 = Dose.of(capacity100, period24);
        Receipe pillsRecipe = Receipe.of(pills, dose24, 2);
        Receipe syrupRecipe = Receipe.of(syrup, dose24, 1);
        MedicinePackage pillsPackage = MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER));
        MedicinePackage syrupPackage = MedicinePackage.of(syrup, Capacity.of(1200, CapacityUnit.MILILITER));
        medicineDoser.add(pillsPackage);
        medicineDoser.add(syrupPackage);
        medicineDoser.dose(pillsRecipe);
        medicineDoser.dose(syrupRecipe);
        InOrder inOrder = Mockito.inOrder(infuser, dosageLog, clock);
        inOrder.verify(dosageLog).logStart();
        inOrder.verify(dosageLog).logStartDose(pills, dose24);
        inOrder.verify(infuser).dispense(pillsPackage, capacity100);
        inOrder.verify(dosageLog).logEndDose(pills, dose24);
        inOrder.verify(clock).wait(period24);
        inOrder.verify(dosageLog).logStartDose(syrup, dose24);
        inOrder.verify(infuser).dispense(syrupPackage, capacity100);
        inOrder.verify(dosageLog).logEndDose(syrup, dose24);
        inOrder.verify(clock).wait(period24);
        inOrder.verify(dosageLog).logEnd();
    }

    @Test
    void catchExceptionInfuserTest() throws InfuserException {
        Receipe pillsRecipe = Receipe.of(pills, Dose.of(capacity100, period12), 1);
        MedicinePackage medicinePackage = MedicinePackage.of(pills, Capacity.of(12, CapacityUnit.LITER));
        Mockito.doThrow(InfuserException.class).when(infuser).dispense(medicinePackage, capacity100);
        medicineDoser.add(medicinePackage);
        medicineDoser.dose(pillsRecipe);
        InOrder inOrder = Mockito.inOrder(infuser, dosageLog, clock);
        inOrder.verify(dosageLog).logStart();
        inOrder.verify(dosageLog).logStartDose(pills, Dose.of(capacity100, period12));
        inOrder.verify(infuser).dispense(medicinePackage, capacity100);
        inOrder.verify(dosageLog).logDifuserError(Dose.of(capacity100, period12), new InfuserException().getMessage());
        inOrder.verify(clock).wait(period12);
        inOrder.verify(dosageLog).logEnd();
    }

}
