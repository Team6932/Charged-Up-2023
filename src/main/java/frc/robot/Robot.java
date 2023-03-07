// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

// 2023 WORM-E Robot Code

  // Notes for robot movement
  // x = left and right (not used)
  // y = forward and backward
  // z = twist (used for turning)

  // Notes for Logitech Gamepad F310 Buttons
  // A = 1, B = 2, X = 3, Y = 4, Left Bumper = 5, Right Bumper = 6, Back = 7, Start = 8, 
  // Left Joystick Button = 9, Right Joystick Button= 10
  // Joysticks have X and Y axes, Triggers are axes

// External Imports
package frc.robot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoSink;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
//import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import com.revrobotics.ColorSensorV3;
import edu.wpi.first.wpilibj.I2C;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
//import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
//import edu.wpi.first.wpilibj2.command.button.JoystickButton;

/* 
import edu.wpi.first.wpilibj.Encoder;

import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
*/

  // Custom Imports
import frc.robot.SMART_Custom_Methods; //not working, don't know why

public class Robot extends TimedRobot {

  // set control variables
  private DifferentialDrive m_myRobot;
  private Joystick m_joystick;
  private Joystick controller;

  // set drive motor variables
  private static final int left1DeviceID = 1; 
  private CANSparkMax m_left1Motor;
  private static final int left2DeviceID = 2;
  private CANSparkMax m_left2Motor;
  private static final int right1DeviceID = 4;
  private CANSparkMax m_right1Motor; 
  private static final int right2DeviceID = 3;
  private CANSparkMax m_right2Motor; 

  // set arm motor variables
  private static final int bot_pivDeviceID = 5; //Bottom of arm motor pivot 
  private CANSparkMax bot_pivMotor;
  private RelativeEncoder bot_pivEncoder;
  private static final int top_pivDeviceID = 6; //Top of arm motor pivot 
  private CANSparkMax top_pivMotor;
  private RelativeEncoder top_pivEncoder;
  private static final int teleDeviceID = 7; //Telescoping section of arm 
  private CANSparkMax teleMotor;
  private static final int grabDeviceID = 8; //Grabber motor for arm 
  private CANSparkMax grabMotor;
  
  // set cameras
  VideoSink server;
  UsbCamera cam0 = CameraServer.startAutomaticCapture(0);
  UsbCamera cam1 = CameraServer.startAutomaticCapture(1);

  // Set LEDs
  private boolean ledToggle;
  private boolean manualPositionToggle;
  private boolean autoPositionToggle;
  private boolean diognosticToggle;

  // Set telescoping
  DigitalInput retractLimit = new DigitalInput(0);
  DigitalInput extendLimit = new DigitalInput(2);

  // Sets autos
  private static final String defaultAuto = "Default";  // Just drives out
  private static final String Auto1 = "Auto1";  // Drives onto charger station and stays on
  private static final String Auto2 = "Auto2";  // Places cube on middle and drives out
  private static final String Auto3 = "Auto3";  // Places cube on high and drives out
  private String autoSelected;
  private final SendableChooser<String> chooser = new SendableChooser<>();
    
  

  // set timer
  double startTime;
  Timer grabTimer;

  @Override
  public void robotInit() {

    // establish drive motor variables
    m_left1Motor = new CANSparkMax(left1DeviceID, MotorType.kBrushed);
    m_left2Motor = new CANSparkMax(left2DeviceID, MotorType.kBrushed);
    m_right1Motor = new CANSparkMax(right1DeviceID, MotorType.kBrushed);
    m_right2Motor = new CANSparkMax(right2DeviceID, MotorType.kBrushed);

    // group Left and Right motors so they move the same direction and speed for the arcade drive
    MotorControllerGroup leftMotor = new MotorControllerGroup(m_left1Motor, m_left2Motor);
    MotorControllerGroup rightMotor = new MotorControllerGroup(m_right1Motor, m_right2Motor);

    // invert right side of the drivetrain so that positive voltage results 
    // in both sides moving forward (forward instead of turn)
    rightMotor.setInverted(true);   
    // drive is set to use the left and right motors
    m_myRobot = new DifferentialDrive(leftMotor, rightMotor);

    // establish arm motor variables
    bot_pivMotor = new CANSparkMax(bot_pivDeviceID, MotorType.kBrushless);
    bot_pivMotor.setInverted(true);
    bot_pivEncoder = bot_pivMotor.getEncoder();
    top_pivMotor = new CANSparkMax(top_pivDeviceID, MotorType.kBrushless);
    top_pivMotor.setInverted(true);
    top_pivEncoder = top_pivMotor.getEncoder();
    teleMotor = new CANSparkMax(teleDeviceID, MotorType.kBrushed);
    grabMotor = new CANSparkMax(grabDeviceID, MotorType.kBrushed);

    // establish controller variablse
    m_joystick = new Joystick(0);
    controller = new Joystick(1);

    // make cameras work
    server = CameraServer.getServer();

    manualPositionToggle = true;
    autoPositionToggle = false;
    diognosticToggle = false;

    bot_pivEncoder.setPosition(0);
    top_pivEncoder.setPosition(0);

    chooser.setDefaultOption("Just Drive Out", defaultAuto);
    chooser.addOption("Over Charge Station", Auto1);
    chooser.addOption("Place Cube on Middle and Drive Out", Auto2);
    chooser.addOption("Place Cube on High and Drive Out", Auto3);
    SmartDashboard.putData("Auto choices", chooser);
  }

  // create functions for arm movement (Will be imported from another file later)
  // (input desired encoder position, encoder position, and motor)
  private void move_to_position(double set_point, double current_point, CANSparkMax motor, double motorspeed, boolean inputCondition) {
    if(current_point<set_point&&inputCondition){
      motor.set(motorspeed);
    }
    else{
      motor.set(0);
    }
  }
  private void move_to_rest(double rest_point, double current_point, CANSparkMax motor, double motorspeed, boolean inputCondition) {
    if(current_point>rest_point&&inputCondition){
      motor.set(motorspeed); //input a negative
    }
    else{
      motor.set(0);
    }
  }

  private void limit_hit(Boolean limit, CANSparkMax motor, double motorspeed, boolean inputCondition) {
    if(limit==false&&inputCondition){
      motor.set(motorspeed);
    }
    else{
      motor.set(0);
    }
  }

  public boolean POVAngle(int angle, Joystick input_device) {
    if(input_device.getPOV()==angle){
      return true;
    }
    else{
      return false;
    }
  }

  public boolean diognosticConditions(boolean inputCondition, boolean toggle) { // Make toggle diognosticToggle whenever called
    if(toggle){
      return true;
    }
    else{
      return inputCondition;
    }
  }

  public void graberMove(String gamePiece, CANSparkMax motor, Timer grabTimer, String state) {
    grabTimer.start(); // might not work
    if(gamePiece=="Cube"||gamePiece=="cube"){
      if(grabTimer.get()!=0&&grabTimer.get()>3&&state=="open"){
        motor.set(0.3);
      }
      else if(grabTimer.get()!=0&&grabTimer.get()>3&&state=="closed"){
        motor.set(-0.3);
      }
      else{
        grabTimer.stop();
        grabTimer.reset();
        motor.set(0);
      }
    }
    else if(gamePiece=="Cone"||gamePiece=="cone"){
      if(grabTimer.get()!=0&&grabTimer.get()>4&&state=="open"){
        motor.set(0.3);
      }
      else if(grabTimer.get()!=0&&grabTimer.get()>4&&state=="closed"){
        motor.set(-0.3);
      }
      else{
        grabTimer.stop();
        grabTimer.reset();
        motor.set(0);
      }
    }
    


  }
  


  @Override
  public void teleopPeriodic() {
  
    // establish variables
    boolean A = controller.getRawButton(1);
    boolean B = controller.getRawButton(2);
    boolean X = controller.getRawButton(3);
    boolean Y = controller.getRawButton(4);
    boolean LB = controller.getRawButton(5);
    boolean RB = controller.getRawButton(6);
    boolean padUp = POVAngle(0, controller);
    boolean padRight = POVAngle(90, controller);
    boolean padDown = POVAngle(180, controller);
    boolean padLeft = POVAngle(270, controller);

    double bot_pivPosition = bot_pivEncoder.getPosition();
    double top_pivPosition = top_pivEncoder.getPosition();

    SmartDashboard.putNumber("Top Pivot Position", top_pivPosition);
    SmartDashboard.putNumber("Bottom Pivot Position", bot_pivPosition);


    /* Untested Slider Code 
    double axis_value = m_joystick.getRawAxis(3);
    double mutliplier = ((axis_value + 1)/2);
    System.out.format("%.2f%n",mutliplier);
    */
    
   // Robot drive 
      // Use joystick for driving
    if(m_joystick.getRawButton(2)){
      m_myRobot.arcadeDrive(-m_joystick.getY()*0.4, m_joystick.getZ()*0.4);
    }
    else{
      m_myRobot.arcadeDrive(-m_joystick.getY(), m_joystick.getZ()*0.5);
    }
    

    // Cameras
      // When the trigger on the joystick is held, display will change from drive camera to arm camera
    if (m_joystick.getRawButtonPressed(1)) {
      System.out.println("Setting camera 0");
      server.setSource(cam0);
    }
    else if (m_joystick.getRawButtonReleased(1)) {
      System.out.println("Setting camera 1");
      server.setSource(cam1);
    }

    // Reset encoder values ("left joystick" button)
    if (controller.getRawButtonPressed(9)){
      bot_pivEncoder.setPosition(0);
      top_pivEncoder.setPosition(0);
    }

    if(controller.getRawButtonPressed(7)){
      manualPositionToggle = true;
      autoPositionToggle = false;
    }

    if(controller.getRawButtonPressed(8)){
      autoPositionToggle = true;
      manualPositionToggle = false;
    }

    if(controller.getRawButtonPressed(10)){
      diognosticToggle = !diognosticToggle;
    }
    
    if(autoPositionToggle){
      if(A||B||X||Y){
        if(A){        // Moves the arm to Floor height scoring/pickup position (A button)
          move_to_position(6, top_pivPosition, top_pivMotor, 0.1,true);
          move_to_position(5, bot_pivPosition, bot_pivMotor, 0.1, top_pivPosition>5);
        }
        else if(B){   // Moves the arm to Medium height scoring position (B button)
          move_to_position(30, top_pivPosition, top_pivMotor, 0.25,true);
          //move_to_position(20, bot_pivPosition, bot_pivMotor, 0.1, top_pivPosition>5);
          limit_hit(extendLimit.get(), teleMotor, 0.75,top_pivPosition>20);      
        }
        else if(X){   // Moves the arm to Shelf pickup position (X button)
          move_to_position(35, top_pivPosition, top_pivMotor, 0.1,true); 
          limit_hit(extendLimit.get(), teleMotor, 0.75,true);       
        }
        else if(Y){   // Moves the arm to High height scoring position (Y button)
          move_to_position(10, bot_pivPosition, bot_pivMotor, 0.1,true);
          move_to_position(10, top_pivPosition, top_pivMotor, 0.1, top_pivPosition>5);
          limit_hit(extendLimit.get(), teleMotor, 0.75,true);        
        }
      }
      else{   // Moves the arm back to its resting position (No button)
        limit_hit(retractLimit.get(), teleMotor, -0.75,true);
        move_to_rest(0, top_pivPosition, top_pivMotor, -0.1,extendLimit.get()==false);
        move_to_rest(0, bot_pivPosition, bot_pivMotor, -0.1,top_pivPosition<5);
      }
    }
    else if(manualPositionToggle){
      if(padUp){
        top_pivMotor.set(0.15);
      }
      else if(padDown&&diognosticConditions(top_pivPosition>0, diognosticToggle)){
        top_pivMotor.set(-0.15);
      }
      else{
        top_pivMotor.set(0);
      }

      if(padLeft){
        bot_pivMotor.set(0.1);
      }
      else if(padRight&&diognosticConditions(bot_pivPosition>0, diognosticToggle)){
        bot_pivMotor.set(-0.1);
      }
      else{
        bot_pivMotor.set(0);
      }
      
      if(LB&&extendLimit.get()==false){
        teleMotor.set(0.5);
      }
      else if(controller.getRawAxis(2)>0.5&&diognosticConditions(retractLimit.get()==false, diognosticToggle)){
        teleMotor.set(-0.5);
      }
      else{
        teleMotor.set(0);
      }
    }
    
    
    if(RB){
      grabMotor.set(0.3);
    }
    else if(controller.getRawAxis(3)>0.5){
      grabMotor.set(-0.3);
    }
    else{
      grabMotor.set(0);
    }
  
    
  } 
  // Autonomous

  @Override
  public void autonomousInit() {
    autoSelected = chooser.getSelected();
    System.out.println("Auto Selected: "+ autoSelected);
    startTime = Timer.getFPGATimestamp();


  }


  @Override
  public void autonomousPeriodic() {
    // establish variables
    double top_pivPosition = top_pivEncoder.getPosition();
    double bot_pivPosition = bot_pivEncoder.getPosition();
    double timeRobot = Timer.getFPGATimestamp();
    switch(autoSelected){
      case Auto1:
        if((timeRobot - startTime > 1) && (timeRobot - startTime < 3.75)) { //put values back to (time > 12) && (time < 15) and add back else
        
          m_myRobot.arcadeDrive(0.4, 0,false); 
        } else {
          m_myRobot.arcadeDrive(0, 0,false); 

        }       
        break;
      case Auto2:
        if(timeRobot-startTime==0&&timeRobot-startTime>5){
          move_to_position(30, top_pivPosition, top_pivMotor, 0.3, true);
          limit_hit(extendLimit.get(), teleMotor, 0.75, top_pivPosition>20);
        }
        else if(timeRobot-startTime<5&&timeRobot-startTime>8){
          graberMove("cube", grabMotor, grabTimer, "closed");
        }
        break;
      case Auto3:

        break;
      case defaultAuto:
        if((timeRobot - startTime > 1) && (timeRobot - startTime < 4)) {
        
          m_myRobot.arcadeDrive(-0.3, 0,false); 
        } else {
          m_myRobot.arcadeDrive(0, 0,false); 

        }      
        break;
    }

  } 
  
}
