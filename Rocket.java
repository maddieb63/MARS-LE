    package mars.mips.instructions.customlangs;
    import mars.simulator.*;
    import mars.mips.hardware.*;
    import mars.mips.instructions.syscalls.*;
    import mars.*;
    import mars.util.*;
    import java.util.*;
    import java.io.*;
    import mars.mips.instructions.*;
    import java.util.Random;

public class Rocket extends CustomAssembly{

    // New register names for Rocket Assmebly
    private static final int REG_ZERO = 0;
    private static final int REG_ALT = 1; // altitude
    private static final int REG_VEL = 2; // velocity
    private static final int REG_OXT = 3; // ox tank
    private static final int REG_FUEL = 4; // fuel tank
    private static final int REG_STAT = 5; // status
    /* Status:
       0 = Nothing
       1 = Ignition
       2 = Launch
       3 = Parachute deployed
       4 = Landed
       5 = Aborted
     */

    
    @Override
    public String getName(){
        return "Rocket";
    }

    @Override
    public String getDescription(){
        return "Launch and fly a Rocket!";
    }

    @Override 
    protected void populate(){ 
        // ------------------ BASIC INSTRUCTIONS -------------
        // LIFT Instruction (add)
        instructionList.add(
            new BasicInstruction("LIFT $rd,$rs,$rt",
                "rd = rs + rt",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss fffff ddddd 00000 100000",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd = operands[0];
                        int rs = operands[1];
                        int rt = operands[2];
                        int a = RegisterFile.getValue(rs);
                        int b = RegisterFile.getValue(rt);
                        int result = a + b;

                        if ((a >= 0 && b >= 0 && result < 0) || (a < 0 && b < 0 && result >= 0)) {
                            throw new ProcessingException(statement, "arithmetic overflow", Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                        }
                        RegisterFile.updateRegister(rd, result);
                    }
                }
            )
        );
        // DROP Instruction (subtract)
        instructionList.add(
            new BasicInstruction("DROP $rd,$rs,$rt",
                "rd = rs - rt",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss fffff ddddd 00000 100010",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd = operands[0];
                        int rs = operands[1];
                        int rt = operands[2];
                        int a = RegisterFile.getValue(rs);
                        int b = RegisterFile.getValue(rt);
                        int result = a - b;

                        RegisterFile.updateRegister(rd, result);
                    }
                }
            )

        );
        // COMBUST Instruction 
        instructionList.add(
            new BasicInstruction("COMBUST $rt,$rs,imm",
                "rt = rs + immediate",
                BasicInstructionFormat.I_FORMAT,
                "001000 fffff ttttt ssssssssssssssss",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rt = operands[0];
                        int rs = operands[1];
                        int imm = operands[1] << 16 >> 16;
                        int result = RegisterFile.getValue(rs) + imm;
                        RegisterFile.updateRegister(rt, result);
                    }
                }
            )
        );
        // LOADFUEL Instruction (lw)
        instructionList.add(
            new BasicInstruction("LOADFUEL $rd,offset($rs)",
                "Load fuel level from memory into register",
                BasicInstructionFormat.I_FORMAT,
                "100011 sssss ddddd oooooooooooooooo",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd = operands[0];
                        int rs = operands[1];
                        int offset = operands[2] << 16 >> 16;
                        int address = RegisterFile.getValue(rs) + offset;
                        int fuelLevel = 0;
                        try {
                            fuelLevel = Globals.memory.getWord(address);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(statement, "address error", 4);
                        }
                        RegisterFile.updateRegister(rd, fuelLevel);
                    }
                }
            )
        );
        // STOREFUEL Instruction (sw)
        instructionList.add(
            new BasicInstruction("STOREFUEL $rd,offset($rs)",
                "Store fuel level from register into memory",
                BasicInstructionFormat.I_FORMAT,
                "101011 sssss ddddd oooooooooooooooo",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd = operands[0];
                        int rs = operands[1];
                        int offset = operands[2] << 16 >> 16;
                        int address = RegisterFile.getValue(rs) + offset;
                        int fuelLevel = RegisterFile.getValue(rd);
                        try {
                            Globals.memory.setWord(address, fuelLevel);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(statement, "address error", 4);
                        }
                    }
                }
            )
        );
        // EQBRANCH Instruction (beq)
        instructionList.add(
            new BasicInstruction("EQBRANCH $rs,$rt,label",
                "Branch if equal",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000100 sssss ttttt oooooooooooooooo",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = operands[0];
                        int rt = operands[1];
                        int label = operands[2];
                        if (RegisterFile.getValue(rs) == RegisterFile.getValue(rt)) {
                            Globals.instructionSet.processBranch(operands[2]);
                        }
                    }
                }
            )
        );
        // LESSBRANCH Instruction (blt)
        instructionList.add(
            new BasicInstruction("LESSBRANCH $rs,$rt,label",
                "Branch if less than",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000111 sssss ttttt oooooooooooooooo",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = operands[0];
                        int rt = operands[1];
                        int label = operands[2];
                        if (RegisterFile.getValue(rs) < RegisterFile.getValue(rt)) {
                            Globals.instructionSet.processBranch(operands[2]);
                        }
                    }
                }
            )
        );
        // WARP Instruction (j)
        instructionList.add(
            new BasicInstruction("WARP label",
                "Jump to label",
                BasicInstructionFormat.J_FORMAT,
                "000011 llllllllllllllllllllllllll",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        Globals.instructionSet.processBranch(operands[0]);
                    }
                }
            )
        );
        // SETLESS Instruction (slt)
        instructionList.add(
            new BasicInstruction("SETLESS $rd,$rs,$rt",
                "Set rd to 1 if rs < rt, else set rd to 0",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss fffff ddddd 00000 101010",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd = operands[0];
                        int rs = operands[1];
                        int rt = operands[2];
                        int aval = RegisterFile.getValue(rs);
                        int bval = RegisterFile.getValue(rt);
                        if (aval < bval) {
                            RegisterFile.updateRegister(rd, 1);
                        } else {
                            RegisterFile.updateRegister(rd, 0);
                        }
                    }
                }
            )
        );
        // NEQBRANCH Instruction (bne)
        instructionList.add(
            new BasicInstruction("NEQBRANCH $rs,$rt,label",
                "Branch if not equal",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000101 sssss fffff llllllllllllllll",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = operands[0];
                        int rt = operands[1];
                        int label = operands[2];
                        if (RegisterFile.getValue(rs) != RegisterFile.getValue(rt)) {
                            Globals.instructionSet.processBranch(operands[2]);
                        }
                    }
                }
            )
        );
        // ----------------- UNIQUE INSTRUCTIONS -------------
        // IGNITE - set STATUS to 1
        instructionList.add(
            new BasicInstruction("IGNITE $rt",
                "Ignite the rocket engines: set STATUS register to 1",
                BasicInstructionFormat.R_FORMAT,
                "011111 00000 00000 rrrrr 00000 000001",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rt = operands[0];
                        RegisterFile.updateRegister(rt, 1); 
                        SystemIO.printString("Rocket ignited. STATUS set to 1\n");
                    }
                }
            )
        );
        // THRUST - increase ALTITUDE by FUEL amount if STATUS is 1
        instructionList.add(
            new BasicInstruction("THRUST $status,$alt,$fuel",
                "Thrust the rocket upwards: increase ALTITUDE if STATUS is 1",
                BasicInstructionFormat.R_FORMAT,
                "011111 sssss ttttt ddddd 00000 000010",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int status = operands[0]; 
                        int altitude = operands[1]; 
                        int fuel = operands[2]; 
                        if (status == 1) {
                            int newAltitude = RegisterFile.getValue(altitude);
                            int newFuel = RegisterFile.getValue(fuel);
                            newAltitude += newFuel;
                            RegisterFile.updateRegister(operands[1], newAltitude);
                            SystemIO.printString("Thrust applied. ALTITUDE increased to " + newAltitude + "\n");
                        }
                    }
                }
            )
        );
        // LAUNCH
        instructionList.add(
            new BasicInstruction("LAUNCH $rt",
                "Launch the rocket: set STATUS register to 2 if STATUS is 1",
                BasicInstructionFormat.R_FORMAT,
                "011111 00000 00000 rrrrr 00000 000011",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rt = operands[0];
                        int status = RegisterFile.getValue(rt);
                        if (status == 1) {
                            RegisterFile.updateRegister(rt, 2); 
                            SystemIO.printString("Rocket launched. STATUS set to 2\n");
                        }
                    }
                }
            )
        );
        // PARACHUTE 
        instructionList.add(
            new BasicInstruction("PARACHUTE $status,$vel",
                "Deploy the parachute: set STATUS register to 3 and reduce velocity",
                BasicInstructionFormat.R_FORMAT,
                "011111 sssss ttttt 00000 00000 000100",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int statusReg = operands[0];
                        int velReg = operands[1];
                        RegisterFile.updateRegister(statusReg, 3);
                        int currentVel = RegisterFile.getValue(velReg);
                        //Reduce velocity
                        int newVel = - (Math.abs(currentVel) / 4);
                        RegisterFile.updateRegister(velReg, newVel);
                        SystemIO.printString("Parachute deployed. Velocity reduced to " + newVel + "\n");
                    }
                }
            )
        );
        // LAND
        instructionList.add(
            new BasicInstruction("LAND $status,$alt,$vel",
                "Land the rocket: Status = 4, velocity = 0, altitude = 0",
                BasicInstructionFormat.R_FORMAT,
                "011111 sssss ttttt ddddd 00000 000101",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int statusReg = operands[0];
                        int altReg = operands[1];
                        int velReg = operands[2];
                        RegisterFile.updateRegister(statusReg, 4);
                        RegisterFile.updateRegister(velReg, 0);
                        RegisterFile.updateRegister(altReg, 0);
                        SystemIO.printString("Rocket landed. Status set to 4, velocity and altitude are 0.\n");
                    }
                }
            )
        );
    }
}
