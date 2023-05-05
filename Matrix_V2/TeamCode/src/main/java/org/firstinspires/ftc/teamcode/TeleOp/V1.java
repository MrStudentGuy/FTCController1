package org.firstinspires.ftc.teamcode.TeleOp;

import static org.firstinspires.ftc.teamcode.TransferClass.turretAngle;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.profile.MotionState;
import com.arcrobotics.ftclib.controller.PIDController;
import com.outoftheboxrobotics.photoncore.PhotonCore;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Subsystems.Lift;
import org.firstinspires.ftc.teamcode.Subsystems.Sensors;
import org.firstinspires.ftc.teamcode.Subsystems.Servos;
import org.firstinspires.ftc.teamcode.Subsystems.Turret;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;

import java.util.Objects;

@TeleOp(name = "-->TELEOP \uD83D\uDC4C\uD83D\uDC4C\uD83D\uDE0D\uD83C\uDFB6\uD83C\uDFB6\uD83D\uDE0E\uD83D\uDE1C\uD83D\uDE2D\uD83E\uDD70\uD83D\uDE08\uD83D\uDC7A\uD83D\uDC7A\uD83E\uDD23\uD83E\uDD23\uD83D\uDE15\uD83D\uDE1C\uD83D\uDE2D\uD83E\uDD70\uD83E\uDD70\uD83D\uDE18")
@Config
public class V1 extends LinearOpMode {
    public static final double Kp = 0.07;
    public static final double Ki = 0;
    public static final double Kd = 0.001;
    public static final double Kf = 0; //feedforward, turret no gravity so 0
    public static double targetDegree = 0;
    final ElapsedTime safetyTimer = new ElapsedTime();
    final ElapsedTime AutoCycleTimer = new ElapsedTime();
    boolean aFlag = false;
    boolean bFlag = false;
    boolean RBFlag = false;
    boolean LBFlag = false;
    boolean AutoCycleFlag = false;
    boolean AutoCycleProceed = false;
    boolean goSafeAfterReleaseFlag = false;
    double pos = 0;
    Lift lift = null;
    Servos servos = null;
    Turret turret = null;
    Sensors sensors = null;
    AutoCycleCenterStates autoCycleCenterStates = AutoCycleCenterStates.IDLE;
    private boolean wristInFlag = false;
    private int liftPos = 0;

    @Override
    public void runOpMode() throws InterruptedException {
        ElapsedTime teleOpTime = new ElapsedTime();
        PhotonCore.enable();
        lift = new Lift(hardwareMap, telemetry);
        servos = new Servos(hardwareMap, telemetry);
        turret = new Turret(hardwareMap, "turret", telemetry);
        sensors = new Sensors(hardwareMap, telemetry);

        //meant for the turret
        PIDController controller = new PIDController(Kp, Ki, Kd);
//        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap, telemetry);

        Pose2d startPose = new Pose2d(0, 0, Math.toRadians(180));
        drive.setPoseEstimate(startPose);
//        drive.setPoseEstimate(new Pose2d(poseStorage.getX(), poseStorage.getY(), poseStorage.getHeading()+offsetpose));
        drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);


        while (opModeInInit()) {
            telemetry.addData("Position: ", lift.getPosition()[0] + "," + lift.getPosition()[1]);
            telemetry.update();
            if (gamepad1.dpad_up) {
                liftPos += 2;
            } else if (gamepad1.dpad_down) {
                liftPos -= 2; ///point of failure
            }
            lift.extendTo(liftPos, 0.5);
        }
        targetDegree = 0;

        liftPos = 0;

        setInitialPositions();
        teleOpTime.reset();
        teleOpTime.reset();
        Servos.AlignBar.inside();
        double turretPower = 0;
        turret.setTargetDegree(0);


        while (opModeIsActive()) {
            controller.setPID(Kp, Ki, Kd);
//        liftController.setPID(Kp_lift, Ki_lift, Kd_lift);

            if(turret.turretProfile != null) {
                MotionState profileState = turret.turretProfile.get(turret.turretProfileTimer.seconds());
                double currentTurretDegree = turret.getDegree();
                double turret_pid = controller.calculate(currentTurretDegree, profileState.getX());
                double ff_turret = 1 * Kf;
                turretPower = ff_turret + turret_pid;
            }

            turret.set(turretPower);

            double drivePowerThrottle, drivePowerStrafe, drivePowerHeading;

            if (gamepad1.right_trigger > 0.3 || gamepad1.left_stick_button || gamepad1.right_stick_button) {       //Turn on slow mode
                drivePowerThrottle = 0.4;
                drivePowerStrafe = 0.4;
                drivePowerHeading = 0.4; //slow down - delicate situation
            } else {
                drivePowerThrottle = 1;
                drivePowerStrafe = 1;
                drivePowerHeading = 0.7; //(turning)
            }


            //Field Oriented Drive
            Pose2d poseEstimate = drive.getPoseEstimate();
            Vector2d input = new Vector2d(-gamepad1.left_stick_y * drivePowerThrottle, -gamepad1.left_stick_x * drivePowerStrafe).rotated(-poseEstimate.getHeading());

            drive.setWeightedDrivePower(new Pose2d(input.getX(), input.getY(), -gamepad1.right_stick_x * drivePowerHeading));
            drive.update();//drive is sample mecanum drive




            boolean UP = gamepad1.dpad_up;
            boolean RIGHT = gamepad1.dpad_right;
            boolean DOWN = gamepad1.dpad_down;
            boolean LEFT = gamepad1.dpad_left;
            boolean RB = gamepad1.right_bumper;
            boolean LB = gamepad1.left_bumper;


            boolean LEFT2 = gamepad2.dpad_left;
            boolean RIGHT2 = gamepad2.dpad_right;

            boolean A2 = gamepad2.a;
            boolean B2 = gamepad2.b;
            boolean X2 = gamepad2.x;
            boolean Y2 = gamepad2.y;


            if (gamepad1.a || wristInFlag) {
                if (lift.getPosition()[0] >= 59) {
                    wristInFlag = false;
                    Servos.Wrist.goInit();
                    Servos.Gripper.closeGripper();
                } else {
                    wristInFlag = true;
                    lift.extendTo(60, 1);
                }
            }


            if (A2) { //incase finished all 18, auto not used, then use auto stack
                Servos.Wrist.goGripping();
                lift.extendTo(lift.AUTO_POSITION[4], 0.8);
            } else if (B2 || gamepad1.triangle) {
                Servos.Wrist.goGripping();
                lift.extendTo(lift.AUTO_POSITION[3], 0.8);
            } else if (Y2) {
                Servos.Wrist.goGripping();
                lift.extendTo(lift.AUTO_POSITION[2], 0.8);
            } else if (X2) {
                Servos.Wrist.goGripping();
                lift.extendTo(lift.AUTO_POSITION[1], 0.8);
            }


            if (gamepad2.right_bumper && !AutoCycleFlag) {
                AutoCycleFlag = true;
                AutoCycleTimer.reset();
                autoCycleCenterStates = AutoCycleCenterStates.GOGRIPPING;
            }
            if (gamepad2.start) {
                autoCycleCenterStates = AutoCycleCenterStates.IDLE;
            }
            if (AutoCycleFlag) {
                switch (autoCycleCenterStates) {
                    case IDLE:
                        AutoCycleFlag = false;
                        AutoCycleProceed = false;
                        turret.setDegree(0);
                        lift.extendToLowPole();
                        Servos.Slider.moveInside();
                        Servos.Gripper.openGripper();
                        break;

                    case GOGRIPPING:
                        AutoCycleFlag = true;
                        Servos.Slider.moveOutside();
                        Servos.Gripper.openGripper();
                        lift.extendToGrippingPosition();
                        turret.setDegree(0);
                        Servos.Slider.moveOutside();
                        if (AutoCycleTimer.milliseconds() > 1400) {
                            Servos.Gripper.closeGripper();
                            Servos.Slider.moveHalfway();
                            autoCycleCenterStates = AutoCycleCenterStates.RETRACT;
                            AutoCycleTimer.reset();
                        }
                        break;

                    case RETRACT:
                        if (gamepad2.right_bumper) {
                            lift.extendToHighPole();
                            sleep(500);
                            turret.setDegreeHighPower(150);
                            sleep(800);
                            turret.setDegree(170);
                            sleep(400);
                            Servos.Slider.moveSlider(0.65);
                            sleep(800);
                            Servos.Wrist.goTop();
                            sleep(200);
                            Servos.Wrist.goGripping();
                            sleep(500);
                            Servos.Gripper.setPosition(1);
//                Servos.AlignBar.inside();
                            sleep(50);
                            Servos.Slider.moveHalfway();
                            sleep(50);
                            autoCycleCenterStates = AutoCycleCenterStates.IDLE;
//                        sleep(500);

                            AutoCycleTimer.reset();
                        } else if (gamepad2.left_bumper) {
                            autoCycleCenterStates = AutoCycleCenterStates.GOGRIPPING;
                            AutoCycleTimer.reset();
                        }
                        break;

                }

            } else {
                Servos.Slider.moveSlider(Math.abs(1 - gamepad1.left_trigger));
            }

            if (UP) {
                Servos.Wrist.goAutoTop();
                lift.extendToHighPole();
                Servos.AlignBar.outside();
            } else if ((RIGHT || gamepad2.dpad_down)) {
                Servos.Wrist.goGripping();
                lift.extendToGrippingPosition();
                Servos.AlignBar.inside();
            } else if (DOWN) {
                Servos.Wrist.goAutoTop();
                lift.extendToLowPole();
                Servos.AlignBar.interMediate();
            } else if (LEFT) {
                Servos.Wrist.goAutoTop();
                lift.extendToMidPole();
                Servos.AlignBar.outside();
            }


            if (gamepad2.start || gamepad1.start) {
                Servos.Gripper.gripperState = "BEACON";
            }

            if (RB && !RBFlag) {
                RBFlag = true; //only once
                if (Objects.equals(Servos.Gripper.gripperState, "OPEN")) {
                    Servos.Wrist.goGripping();
                    Servos.Gripper.closeGripper();
                } else if (Objects.equals(Servos.Gripper.gripperState, "CLOSED")) {


                    if (lift.getPosition()[0] > lift.POSITIONS[lift.LOW_POLE] - 30) {
                        Servos.Wrist.goGripping();


                        goSafeAfterReleaseFlag = true;
                        safetyTimer.reset();
                    } else {
                        Servos.Gripper.openGripper();
                    }

                } else if (Objects.equals(Servos.Gripper.gripperState, "BEACON")) {
                    Servos.Gripper.gripBeacon();
                }
            }

            if (LB && !LBFlag) {
                LBFlag = true;
                if (Objects.equals(Servos.Wrist.wristState, "GRIPPING")) {
                    Servos.Wrist.goAutoTop();
                    if (lift.getPosition()[0] < lift.POSITIONS[lift.LOW_POLE]) {
                        Servos.AlignBar.interMediate();
                    } else {
                        Servos.AlignBar.outside();
                    }
                } else if (Objects.equals(Servos.Wrist.wristState, "TOP")) {
                    Servos.AlignBar.inside();
                    Servos.Wrist.goGripping();
                } else {
                    Servos.Wrist.goGripping();
                }
            }
            telemetry.addData("LbFlag", LBFlag);

            if (!RB) {
                RBFlag = false;
            }
            if (!LB) {
                LBFlag = false;
            }
            if ((LEFT2) && !aFlag) {
//            if ((gamepad1.square || LEFT2) && !aFlag) {
                aFlag = true;
                turret.setTargetDegree(targetDegree += 90);

            } else if (!LEFT2) {
//            } else if (!gamepad1.square && !LEFT2) {
                aFlag = false;
            }
            if (RIGHT2 && !bFlag) {
//            if ((B || RIGHT2) && !bFlag) {
                bFlag = true;
                turret.setTargetDegree(targetDegree-=90);
            } else if (!RIGHT2) {
//            } else if (!B && !RIGHT2) {
                bFlag = false;
            }

            if (gamepad1.touchpad && lift.getPosition()[0] >= lift.POSITIONS[lift.MID_POLE]) {
                targetDegree = 0;
            }


            if (goSafeAfterReleaseFlag) {
                if (safetyTimer.milliseconds() >= 400 && safetyTimer.milliseconds() < 600) {
                    Servos.Gripper.openGripper();
                }

                if (safetyTimer.milliseconds() >= 400 && safetyTimer.milliseconds() < 1200) {
                    if (lift.getPosition()[0] > lift.POSITIONS[lift.LOW_POLE]) {
                        targetDegree = 0;
                    }
                    if (safetyTimer.milliseconds() >= 800 && safetyTimer.milliseconds() < 1000) {
                        Servos.Gripper.closeGripper();
                        Servos.AlignBar.inside();
                    }
                    if (safetyTimer.milliseconds() >= 1000) {
                        lift.extendTo(lift.LOW_POLE, 1);
                        goSafeAfterReleaseFlag = false;
                    }
                }
            }

            telemetry.addData("Align Sensor: ", sensors.read());
            telemetry.addData("Currents: ", lift.getCurrent()[0] + ", " + lift.getCurrent()[1]);
            telemetry.addData("Positions: ", lift.getPosition()[0] + ", " + lift.getPosition()[1]);
            telemetry.addData("x", poseEstimate.getX());
            telemetry.addData("y", poseEstimate.getY());
            telemetry.addData("heading", poseEstimate.getHeading());
            telemetry.addData("Turret Angle: ", turret.getDegree());
            telemetry.update();
        }
    }

    private void setInitialPositions() {
        lift.extendTo(0, 0.5);
        Servos.Gripper.closeGripper();
        sleep(30);
        Servos.Wrist.goInit();
        pos = 0;
        turret.setDegree(0);
    }


    @Deprecated
    private void calibrateTurret() {
        double currentDelta = turret.getDegree() - turretAngle;
        if (Math.abs(currentDelta) > 1) {                           //if the current error has an absolute value of greater than 1 degree, rotate the turret in the appropriate direction
            turret.setDegreeHighPower(currentDelta);
        }
        sleep(1000);          //give the turret time to reach it's target
        turret.reset();                 //reset so that telop has a calibrated turret
    }

    enum AutoCycleCenterStates {
        IDLE, GOGRIPPING, RETRACT, DROPANTICLOCKWISE, DROPCLOCKWISE, RETURN
    }
}
