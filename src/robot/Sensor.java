/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robot;

import java.util.ArrayList;
import map.Grid;
import map.Map;
import map.Constants;
import robot.RobotConstant;
import robot.RobotConstant.DIRECTION;

/**
 *
 * @author Chengwei
 */
public class Sensor {

    // Range (In grids)
    private int _minRange;
    private int _maxRange;

    // Sensor's position on the map (In grids)
    private int _sensorPosRow;
    private int _sensorPosCol;

//    private ArrayList<Integer> _frontLeftSensor;
//    private ArrayList<Integer> _frontRightSensor;
//    private ArrayList<Integer> _frontSensor;
//    private ArrayList<Integer> _leftFrontSensor;
//    private ArrayList<Integer> _leftBackSensor;
//    private ArrayList<Integer> _rightSensor;
    // Sensor's current direction
    private DIRECTION _sensorDirection;

    public Sensor(int minRange, int maxRange, int sensorPosRow, int sensorPosCol, DIRECTION sensorDirection) {
        _minRange = minRange;
        _maxRange = maxRange;

        _sensorPosRow = sensorPosRow;
        _sensorPosCol = sensorPosCol;

        _sensorDirection = sensorDirection;
    }

    public int getMinRange() {
        return _minRange;
    }

    public int getMaxRange() {
        return _maxRange;
    }

    public void setMinRange(int newMinRange) {
        _minRange = newMinRange;
    }

    public void setMaxRange(int newMaxRange) {
        _maxRange = newMaxRange;
    }

    public int getSensorPosRow() {
        return _sensorPosRow;
    }

    public int getSensorPosCol() {
        return _sensorPosCol;
    }

    public void setSensorDirection(DIRECTION sensorDirection) {
        _sensorDirection = sensorDirection;
    }

    public DIRECTION getSensorDirection() {
        return _sensorDirection;
    }

    public void updateSensorPos(int newSensorPosRow, int newSensorPosCol) {
        _sensorPosRow = newSensorPosRow;
        _sensorPosCol = newSensorPosCol;
    }

    public void updateSensorDirection(DIRECTION newDirection) {
        _sensorDirection = newDirection;
    }

    public int sense(final Map map) {

        final Grid[][] mapGrids = map.getMapGrids();

        for (int currGrid = _minRange; currGrid <= _maxRange; currGrid++) {
            switch (_sensorDirection) {

                case NORTH:
                    //System.out.println("Checking " + (_sensorPosRow - currGrid) + ", " + _sensorPosCol + ".. ");
                    // Reached top limit of map without detecting any obstacle
//                    if(_minRange >= 1){
//                        for(int i = 0 ; i < _minRange; i++){
//                            if(mapGrids[_sensorPosRow-i][_sensorPosCol].isObstacle())
//                                return 0;
//                        }
//                    }
                    for (int i = 0; i < _minRange; i++) {
                        if (_sensorPosRow - i > 0 && mapGrids[_sensorPosRow - i][_sensorPosCol].isObstacle()) {
                            return 0;
                        }
                    }
                    if ((_sensorPosRow - currGrid) < 0) {
                        return currGrid;
                    } else if (mapGrids[_sensorPosRow - currGrid][_sensorPosCol].isObstacle()) {
                        return currGrid - 1; // Return number of free grids for this direction
                    }
                    break;

                case SOUTH:
                    //System.out.println("Checking " + (_sensorPosRow + currGrid) + ", " + _sensorPosCol + ".. ");
                    // Reached bottom limit of map without detecting any obstacle
                    for (int i = 0; i < _minRange; i++) {
                        if (_sensorPosRow + i < 20 && mapGrids[_sensorPosRow + i][_sensorPosCol].isObstacle()) {
                            return 0;
                        }
                    }
                    if ((_sensorPosRow + currGrid) > (Constants.MAP_ROWS - 1)) {
                        return currGrid;
                    } else if (mapGrids[_sensorPosRow + currGrid][_sensorPosCol].isObstacle()) {
                        return currGrid - 1; // Return number of free grids for this direction
                    }
                    break;

                case EAST:
                    //System.out.println("Checking " + _sensorPosRow + ", " + (_sensorPosCol + currGrid) + ".. ");
                    // Reached right limit of map without detecting any obstacle
                    for (int i = 0; i < _minRange; i++) {
                        if (_sensorPosCol + i < 15 && mapGrids[_sensorPosRow][_sensorPosCol + i].isObstacle()) {
                            return 0;
                        }
                    }
                    if ((_sensorPosCol + currGrid) > (Constants.MAP_COLS - 1)) {
                        return currGrid;
                    } else if (mapGrids[_sensorPosRow][_sensorPosCol + currGrid].isObstacle()) {
                        return currGrid - 1; // Return number of free grids for this direction
                    }
                    break;

                case WEST:
                    //System.out.println("Checking " + _sensorPosRow + ", " + (_sensorPosCol - currGrid) + ".. ");
                    // Reached left limit of map without detecting any obstacle
                    for (int i = 0; i < _minRange; i++) {
                        if (_sensorPosCol - i > 0 && mapGrids[_sensorPosRow][_sensorPosCol - i].isObstacle()) {
                            return 0;
                        }
                    }
                    if ((_sensorPosCol - currGrid) < 0) {
                        return currGrid;
                    } else if (mapGrids[_sensorPosRow][_sensorPosCol - currGrid].isObstacle()) {
                        return currGrid - 1; // Return number of free grids for this direction
                    }
                    break;

            } // End switch
        } // End for loop

        // No obstacles detected within the sensor's maximum range
        // Allow the robot to mark those grids as free
        return _maxRange;
    }

    public void printSensorInfo() {
        System.out.println("Sensor Position (row, col): " + _sensorPosRow
                + ", " + _sensorPosCol);
        System.out.println("Sensor Range (min, max): " + _minRange + ", "
                + _maxRange);
        System.out.println("Sensor Direction: " + _sensorDirection.toString());
    }

}
