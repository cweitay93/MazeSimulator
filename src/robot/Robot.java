/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;
import java.util.Stack;

import javax.swing.Timer;

import map.Grid;
import map.Constants;
import map.MapUI;
import robot.RobotConstant.DIRECTION;
import robot.Sensor;

public class Robot {

    int currentCol, currentRow;
    DIRECTION direction = null;

    // Robot's starting direction
    private transient DIRECTION _robotStartDir;

    // Robot's robot map
    private transient RobotMap _robotMap = null; // For determining next action
    private transient MapUI _mapUI = null; // For detecting obstacles
    ArrayList<Integer> previousCol;
    ArrayList<Integer> previousRow;

    // Robot's collection of sensors
    private ArrayList<Sensor> _sensors = null;

    // Robot's settings for exploration
    private int _stepsPerSecond = RobotConstant.DEFAULT_STEPS_PER_SECOND;
    private int _coverageLimit = RobotConstant.DEFAULT_COVERAGE_LIMIT;
    private int _timeLimit = RobotConstant.DEFAULT_TIME_LIMIT;
    private boolean _bCoverageLimited = false;
    private boolean _bTimeLimited = false;

    // Some memory for the robot here
    private transient boolean _bPreviousLeftWall = false;
    private transient boolean _bReachedGoal = false;
    private transient boolean _bExplorationComplete = false;

    // Timer for controlling robot movement
    private transient Timer _exploreTimer = null;
    private transient Timer _shortestPathTimer = null;
    private transient int _timerIntervals = 0;

    // Number of explored grids required to reach coverage limit
    private transient int _explorationTarget = 0;

    // Elapsed time for the exploration phase (in milliseconds)
    private transient int _elapsedExplorationTime = 0;
    private transient int _elapsedShortestPathTime = 0;
    private DIRECTION midDirection;
    int G = 0;
    int j = 0;
    int k = 0;

    private transient Stack<Grid> _unexploredGrids = null;

    public Robot(int currentRow, int currentCol, DIRECTION direction) {
        this.currentRow = currentRow;
        this.currentCol = currentCol;
        this.direction = direction;

        _sensors = new ArrayList<Sensor>();
    }

    public void setRobotMap(RobotMap robotMap) {
        _robotMap = robotMap;

        // Pass a reference of the robot to the robot map
        // Just for rendering purposes
        _robotMap.setRobot(this);
    }

    public void resetRobotState(int startMapPosRow, int startMapPosCol, DIRECTION startDir) {

        _robotStartDir = startDir;

        // Turn the robot to match the specified starting direction
        while (direction != startDir) {
            this.rotateRight();
        }

        // Update the robot's position to match the specified starting position
        this.updatePosition(currentRow, currentCol, startMapPosRow, startMapPosCol);

        // Reset variables used for exploration
        _bReachedGoal = false;
        _bExplorationComplete = false;
        _bPreviousLeftWall = false;
    }

    public void setTimeLimited() {
        if (!_bTimeLimited) {
            _bTimeLimited = true;
            System.out.println("Time Limited: ON");
        } else {
            _bTimeLimited = false;
            System.out.println("Time Limited: OFF");
        }
    }

    public void setCoverageLimited() {
        if (!_bCoverageLimited) {
            _bCoverageLimited = true;
            System.out.println("Coverage Limited: ON");
        } else {
            _bCoverageLimited = false;
            System.out.println("Coverage Limited: OFF");
        }
    }

    private boolean coverageLimitReached() {

        Grid[][] map = _robotMap.getMapGrids();
        int noOfExploredGrids = 0;

        for (int i = 1; i < Constants.MAP_ROWS; i++) {
            for (int j = 1; j < Constants.MAP_COLS; j++) {
                if (map[i][j].isExplored()) {
                    noOfExploredGrids++;
                }
            }
        }

        return noOfExploredGrids >= _explorationTarget;
    }

    public void setMapUI(final MapUI mapUI) {
        _mapUI = mapUI;
    }

    public void startExplore() {
        // Calculate timer intervals based on the user selected steps per second
        _timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
        System.out.println("Steps Per Second: " + _stepsPerSecond
                + ", Timer Interval: " + _timerIntervals);

        // Calculate number of explored grids required
        _explorationTarget = (int) ((_coverageLimit / 100.0) * ((Constants.MAP_ROWS) * (Constants.MAP_COLS)));
        System.out.println("Exploration target (In grids): "
                + _explorationTarget);

        // Simulate virtual sensors
        _sensors = simulateSensors(currentRow, currentCol, direction);
//        _sensors.stream().forEach((s) -> {
//            s.printSensorInfo();
//        });

        // Reset the elapsed exploration time (in milliseconds)
        _elapsedExplorationTime = 0;

        _exploreTimer = new Timer(_timerIntervals, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (_exploreTimer != null && _bExplorationComplete) {
                    _exploreTimer.stop();
                    _exploreTimer = null;
                } else {
                    // Make the next move
                    makeNextMove();

                    // Update elapsed time
                    _elapsedExplorationTime += _timerIntervals;
                }
            }
        });
        _exploreTimer.setRepeats(true);
        _exploreTimer.setInitialDelay(1000);
        _exploreTimer.start();
    }

    public void stopExploration() {
        if (_exploreTimer != null) {
            _exploreTimer.stop();
            _exploreTimer = null;
        }
    }

    public void makeNextMove() {
        // Sense its surroundings
        this.sense();

        _robotMap.revalidate();
        _robotMap.repaint();

        // Logic to make the next move
        //this.logic();
        this.PossibleMove();
    }

    public void sense() {

        // Weightage of the sensors
        double[] sensorWeightage = {3.0, 3.0, 3.0, 1.5, 1.0, 1.0};

        int sensorIndex = 0;

        for (Sensor s : _sensors) {
            int freeGrids = s.sense(_mapUI);
            int sensorPosRow = s.getSensorPosRow();
            int sensorPosCol = s.getSensorPosCol();
            DIRECTION sensorDir = s.getSensorDirection();
            int sensorMinRange = s.getMinRange();
            int sensorMaxRange = s.getMaxRange();
//            int u = 0;
//            System.out.println(u++ + ".");
//            System.out.println("sensorPosRow: " + sensorPosRow + "sensorPosCol: " + sensorPosCol);
//            System.out.println("sensorDir: " + sensorDir);
//            System.out.println("freeGrids: " + freeGrids);
            /*
			 * System.out.println("Sensor - " + sensorPosRow + ", " +
			 * sensorPosCol + ", " + sensorDir.toString() + ", Free Grids: " +
			 * freeGrids);
             */
            Grid[][] robotMapGrids = _robotMap.getMapGrids();
//            if (sensorMinRange > 1 && freeGrids > 0) {
//                sensorMinRange = 1;
//            }
            if (sensorMinRange >= 1 && freeGrids > 0) {
                sensorMinRange = 1;
                for (int currGrid = sensorMinRange; currGrid <= sensorMaxRange; currGrid++) {
                    int gridRow = sensorPosRow
                            + ((sensorDir == DIRECTION.NORTH) ? (-1 * currGrid)
                                    : (sensorDir == DIRECTION.SOUTH) ? currGrid : 0);
                    int gridCol = sensorPosCol
                            + ((sensorDir == DIRECTION.WEST) ? (-1 * currGrid)
                                    : (sensorDir == DIRECTION.EAST) ? currGrid : 0);
//                    System.out.println("gridRow: " + gridRow + " gridCol: " + gridCol);
                    double truthValue = 1.0 / ((double) currGrid);
                    truthValue *= sensorWeightage[sensorIndex];
                    truthValue *= sensorWeightage[sensorIndex];
//                    System.out.println("currGrid: " + currGrid + " freeGrids: " + freeGrids);
                    // If the current grid is within number of free grids detected
                    if (currGrid <= freeGrids) {
                        if ((gridRow >= 0 && gridRow < 20) && (gridCol >= 0 && gridCol < 15)) {
                            robotMapGrids[gridRow][gridCol].setExplored(true);
                        }

                    } else {

                        // Current grid is less than or equal to max sensor range,
                        // but greater than number of free grids
                        // i.e. current grid is an obstacle
                        //robotMapGrids[gridRow][gridCol].setExplored(true);
                        if (!_robotMap.isStartZone(gridRow, gridCol)
                                && !_robotMap.isGoalZone(gridRow, gridCol)) {
                            if ((gridRow >= 0 && gridRow < 20) && (gridCol >= 0 && gridCol < 15)) {
                                robotMapGrids[gridRow][gridCol].markAsObstacle();
                            }
                        }

                        break;
                    }
                }
            } else {
                int gridRow = sensorPosRow
                        + ((sensorDir == DIRECTION.NORTH) ? (-1)
                                : (sensorDir == DIRECTION.SOUTH) ? 1 : 0);
                int gridCol = sensorPosCol
                        + ((sensorDir == DIRECTION.WEST) ? (-1)
                                : (sensorDir == DIRECTION.EAST) ? 1 : 0);
                if ((gridRow >= 0 && gridRow < 20) && (gridCol >= 0 && gridCol < 15)) {
                    robotMapGrids[gridRow][gridCol].markAsObstacle();
                }
            }
        }
    }

    public void PossibleMove() {
        int oldCol, oldRow, newCol, newRow;

        oldCol = currentCol;
        oldRow = currentRow;

        // Robot reached goal zone
        if (withinGoalZone(currentRow, currentCol)) {
            _bReachedGoal = true;
        }

        if (_bCoverageLimited) {
            if (coverageLimitReached()) {

                // Stop exploration
                _bExplorationComplete = true;

                // Start the shortest path back to the starting grid
                String map = _robotMap.generateShortestPathMap();
                int mapBit[] = new int[300];
                int currentGrid = ConvertToIndex(currentRow, currentCol);
                int startingGrid = ConvertToIndex(0, 0);
                String temp;                                                                                        //Temporary storage for 1 Hex to 4 Bit conversion                                                                                    
                String bitString = "";
                ArrayList<Integer> shortestPath;
                for (int i = 0; i < 76; i++) {
                    temp = String.format("%04d", Integer.parseInt(hexToBin(map.substring(i, i + 1))));                //Hexadecimal to Binary conversion
                    bitString += temp;
                }

                bitString = bitString.substring(2, bitString.length() - 2);                                           //Remove first 2 bit and last 2 bit of the String  

                for (int h = 0; h < 300; h++) {
                    mapBit[h] = Integer.parseInt(bitString.substring(h, h + 1));                                      //String to Int Conversion
                }

                shortestPath = aStarSearch(currentGrid, startingGrid, mapBit, direction);
                shortestPath.add(currentGrid);
                Collections.reverse(shortestPath);
                System.out.println("Shortest Path" + shortestPath);
                simulateShortestPath(shortestPath);
                return;
            }
        }

        if (_bTimeLimited) {
            if ((_elapsedExplorationTime / 1000) >= _timeLimit) {

                // Stop exploration
                _bExplorationComplete = true;

                // Start the shortest path back to the starting grid
                String map = _robotMap.generateShortestPathMap();
                int mapBit[] = new int[300];
                int currentGrid = ConvertToIndex(currentRow, currentCol);
                int startingGrid = ConvertToIndex(0, 0);
                String temp;                                                                                        //Temporary storage for 1 Hex to 4 Bit conversion                                                                                    
                String bitString = "";
                ArrayList<Integer> shortestPath;

                for (int i = 0; i < 76; i++) {
                    temp = String.format("%04d", Integer.parseInt(hexToBin(map.substring(i, i + 1))));                //Hexadecimal to Binary conversion
                    bitString += temp;
                }

                bitString = bitString.substring(2, bitString.length() - 2);                                           //Remove first 2 bit and last 2 bit of the String  

                for (int h = 0; h < 300; h++) {
                    mapBit[h] = Integer.parseInt(bitString.substring(h, h + 1));                                      //String to Int Conversion
                }

                shortestPath = aStarSearch(currentGrid, startingGrid, mapBit, direction);
                shortestPath.add(currentGrid);
                Collections.reverse(shortestPath);
                System.out.println("Shortest Path" + shortestPath);
                simulateShortestPath(shortestPath);
                return;
            }
        }

        if (_bReachedGoal && withinStartZone(currentRow, currentCol)) {

            _bExplorationComplete = true;
            System.out.println("MDF String part 1:" + _robotMap.generateMDFStringPart1());
            System.out.println("MDF String part 2:" + _robotMap.generateMDFStringPart2());
            System.out.println("MDF String:" + _mapUI.generateMapString());
            return;
        }

        // Exploration complete, do nothing
        if (_bExplorationComplete) {
            return;
        }

        // Robot reached goal zone
        if (withinGoalZone(currentRow, currentCol)) {
            _bReachedGoal = true;
        }

        Grid[][] map = _robotMap.getMapGrids();
        int checkExploredGrids = 0;

        for (int i = 1; i < Constants.MAP_ROWS; i++) {
            for (int j = 1; j < Constants.MAP_COLS; j++) {
                if (map[i][j].isExplored()) {
                    checkExploredGrids++;
                }
            }
        }

        if (checkExploredGrids >= 300) {
            _unexploredGrids = getUnexploredGrids();
            if (!_unexploredGrids.isEmpty()) {

                // Start shortest path to the first unexplored grid
                Grid[][] robotMap = _robotMap.getMapGrids();
                Grid currentGrid = robotMap[currentRow][currentCol];

                startExploringUnexplored(currentGrid, direction, _unexploredGrids.pop(), robotMap);
            }
        } else {
            boolean frontWall = isFrontWall();
            boolean leftWall = isLeftWall();
            boolean rightWall = isRightWall();

            // (No leftWall AND previousLeftWall) OR (frontWall AND No leftWall AND rightWall)
            if ((!leftWall && _bPreviousLeftWall) || (frontWall && !leftWall && rightWall)) {
                rotateLeft();
            } // (frontWall AND No rightWall)
            else if (frontWall && !rightWall) {
                rotateRight();
            } // (frontWall AND leftWall AND rightWall)
            else if (frontWall && leftWall && rightWall) {
                rotate180();
            } else {
                moveForward();
                newCol = currentCol;
                newRow = currentRow;
                updatePosition(oldRow, oldCol, newRow, newCol);
            }

            // Save current leftWall state into _bPreviousLeftWall
            _bPreviousLeftWall = leftWall;
        }
    }

    /**
     * For exploring any unexplored area
     */
    public void startExploringUnexplored(Grid current, DIRECTION curDir,
            Grid target, Grid[][] robotMap) {
        while (current != target || !target.isExplored()) {
            //move closer to target grid
        }
    }

    private void updatePosition(int oldRow, int oldCol, int newRow, int newCol) {

        // Determine the change in row/column of the robot
        int deltaRow = newRow - oldRow;
        int deltaCol = newCol - oldCol;

        // Update the path in the robot map
        RobotMap.PathGrid[][] pathGrids = null;
        if (_robotMap != null) {
            pathGrids = _robotMap.getPathGrids();
        }
        if (pathGrids != null) {
            switch (direction) {
                case EAST:
                    pathGrids[oldRow][oldCol].cE = true;
                    pathGrids[newRow][newCol].cW = true;
                    break;
                case NORTH:
                    pathGrids[oldRow][oldCol].cN = true;
                    pathGrids[newRow][newCol].cS = true;
                    break;
                case SOUTH:
                    pathGrids[oldRow][oldCol].cS = true;
                    pathGrids[newRow][newCol].cN = true;
                    break;
                case WEST:
                    pathGrids[oldRow][oldCol].cW = true;
                    pathGrids[newRow][newCol].cE = true;
                    break;
            }
        }

        // Update the actual position of the robot
        currentRow = newRow;
        currentCol = newCol;

        // Update the positions of the sensors
        for (Sensor s : _sensors) {
            s.updateSensorPos(s.getSensorPosRow() + deltaRow,
                    s.getSensorPosCol() + deltaCol);
        }
    }

    private void turn(boolean bClockwise) {

        // Center of robot
        int xC = 0;
        int yC = 0;

        // Determine the center of the robot based on its current position
        xC = (currentCol * Constants.GRID_SIZE)
                + (RobotConstant.ROBOT_SIZE * Constants.GRID_SIZE / 2);
        yC = (currentRow * Constants.GRID_SIZE)
                + (RobotConstant.ROBOT_SIZE * Constants.GRID_SIZE / 2);

        // x = ((x - x_origin) * cos(angle)) - ((y_origin - y) * sin(angle)) +
        // x_origin
        // y = ((y_origin - y) * cos(angle)) - ((x - x_origin) * sin(angle)) +
        // y_origin
        // Rotate sensors
        for (Sensor s : _sensors) {
            int s_xC = (s.getSensorPosCol() * Constants.GRID_SIZE)
                    + (Constants.GRID_SIZE / 2);
            int s_yC = (s.getSensorPosRow() * Constants.GRID_SIZE)
                    + (Constants.GRID_SIZE / 2);

            // 90 degrees rotation
            double angle = Math.PI / 2.0;
            if (bClockwise) {
                angle *= -1;
            }

            double new_s_xC = ((s_xC - xC) * Math.cos(angle))
                    - ((yC - s_yC) * Math.sin(angle)) + xC;
            double new_s_yC = ((yC - s_yC) * Math.cos(angle))
                    - ((s_xC - xC) * Math.sin(angle)) + yC;

            int newSensorPosCol = (int) (new_s_xC / Constants.GRID_SIZE);
            int newSensorPosRow = (int) (new_s_yC / Constants.GRID_SIZE);

            s.updateSensorPos(newSensorPosRow, newSensorPosCol);
            s.updateSensorDirection(bClockwise ? DIRECTION.getNext(s
                    .getSensorDirection()) : DIRECTION.getPrevious(s
                            .getSensorDirection()));
        }

        // Rotate the robot
        direction = bClockwise ? DIRECTION.getNext(direction)
                : DIRECTION.getPrevious(direction);
    }

    private Stack<Grid> getUnexploredGrids() {
        Stack<Grid> unexploredGrids = new Stack<Grid>();

        Grid[][] map = _robotMap.getMapGrids();
        for (int i = 1; i < Constants.MAP_ROWS - 1; i++) {
            for (int j = 1; j < Constants.MAP_COLS - 1; j++) {
                if (!map[i][j].isExplored()) {
                    unexploredGrids.push(map[i][j]);
                }
            }
        }

//		if (!unexploredGrids.isEmpty()) {
//			Collections.reverse(unexploredGrids);
//		}
        return unexploredGrids;
    }

    private boolean withinStartZone(int robotMapPosRow, int robotMapPosCol) {

        // Check if the entire robot is within the start zone
        Grid[][] robotMapGrids = _robotMap.getMapGrids();
        for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
                + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
                    + RobotConstant.ROBOT_SIZE; mapCol++) {

                if (!robotMapGrids[mapRow][mapCol].isExplored()) {
                    return false;
                } else if (!_robotMap.isStartZone(mapRow, mapCol)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean withinGoalZone(int robotMapPosRow, int robotMapPosCol) {

        // Check if the entire robot is within the start zone
        Grid[][] robotMapGrids = _robotMap.getMapGrids();
        for (int mapRow = robotMapPosRow; mapRow < robotMapPosRow
                + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = robotMapPosCol; mapCol < robotMapPosCol
                    + RobotConstant.ROBOT_SIZE; mapCol++) {

                if (!robotMapGrids[mapRow][mapCol].isExplored()) {
                    return false;
                } else if (!_robotMap.isGoalZone(mapRow, mapCol)) {
                    return false;
                }
            }
        }
        return true;
    }

//===============================================================Conversion Of Index =======================================================================
    public int ConvertToIndex(int currentRow, int currentCol) {
        return currentCol + (currentRow * 15);
    }

    public int ConvertCol(int currentIndex) {
        return currentIndex % 15;
    }

    public int ConvertRow(int currentIndex) {
        return currentIndex / 15;
    }

//=====================================================================Check Wall============================================================================     
    public boolean isLeftWall() {
        int leftWallX, leftWallY;

        Grid[][] _grids = _robotMap.getMapGrids();

        switch (direction) {
            case NORTH:
                leftWallX = currentCol - 1;
                leftWallY = currentRow;

                if (leftWallX < 0) {
                    return true;
                } else if (leftWallX >= 0) {
                    for (int i = leftWallY; i < leftWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][leftWallX].isExplored() && _grids[i][leftWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case EAST:

                leftWallX = currentCol;
                leftWallY = currentRow - 1;

                if (leftWallY < 0) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallX; i < leftWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[leftWallY][i].isExplored() && _grids[leftWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case WEST:
                leftWallX = currentCol;
                leftWallY = currentRow + RobotConstant.ROBOT_SIZE;

                if (leftWallY > 19) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallX; i < leftWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[leftWallY][i].isExplored() && _grids[leftWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case SOUTH:
                leftWallX = currentCol + RobotConstant.ROBOT_SIZE;
                leftWallY = currentRow;

                if (leftWallX > 14) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallY; i < leftWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][leftWallX].isExplored() && _grids[i][leftWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            default:
                return false;
        }
    }

    public boolean isRightWall() {
        int leftWallX, leftWallY;

        Grid[][] _grids = _robotMap.getMapGrids();

        switch (direction) {
            case NORTH:
                leftWallX = currentCol + RobotConstant.ROBOT_SIZE;
                leftWallY = currentRow;

                if (leftWallX > 14) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallY; i < leftWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][leftWallX].isExplored() && _grids[i][leftWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case EAST:
                leftWallX = currentCol;
                leftWallY = currentRow + RobotConstant.ROBOT_SIZE;

                if (leftWallY > 19) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallX; i < leftWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[leftWallY][i].isExplored() && _grids[leftWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case WEST:
                leftWallX = currentCol;
                leftWallY = currentRow - 1;

                if (leftWallY < 0) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallX; i < leftWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[leftWallY][i].isExplored() && _grids[leftWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case SOUTH:
                leftWallX = currentCol - 1;
                leftWallY = currentRow;

                if (leftWallX < 0) {
                    return true;
                } else if (leftWallY >= 0) {
                    for (int i = leftWallY; i < leftWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][leftWallX].isExplored() && _grids[i][leftWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            default:
                return false;
        }
    }

    public boolean isFrontWall() {
        int frontWallX, frontWallY;

        Grid[][] _grids = _robotMap.getMapGrids();

        switch (direction) {
            case NORTH:
                frontWallX = currentCol;
                frontWallY = currentRow - 1;

                if (frontWallY < 0) {
                    return true;
                } else if (frontWallY >= 0) {
                    for (int i = frontWallX; i < frontWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[frontWallY][i].isExplored() && _grids[frontWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }

            case EAST:
                frontWallX = currentCol + RobotConstant.ROBOT_SIZE;
                frontWallY = currentRow;

                if (frontWallX > 14) {
                    return true;
                } else if (frontWallX >= 0) {
                    for (int i = frontWallY; i < frontWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][frontWallX].isExplored() && _grids[i][frontWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case WEST:
                frontWallX = currentCol - 1;
                frontWallY = currentRow;

                if (frontWallX < 0) {
                    return true;
                } else if (frontWallX >= 0) {
                    for (int i = frontWallY; i < frontWallY + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[i][frontWallX].isExplored() && _grids[i][frontWallX].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            case SOUTH:
                frontWallX = currentCol;
                frontWallY = currentRow + RobotConstant.ROBOT_SIZE;

                if (frontWallY > 19) {
                    return true;
                } else if (frontWallY >= 0) {
                    for (int i = frontWallX; i < frontWallX + RobotConstant.ROBOT_SIZE; i++) {
                        if (_grids[frontWallY][i].isExplored() && _grids[frontWallY][i].isObstacle()) {
                            return true;
                        }
                    }
                    return false;
                }
            default:
                return false;
        }
    }
//=============================================================For Movement===============================================================================

    public void moveForward() {
        switch (direction) {
            case NORTH:
                currentRow -= 1;
                markCurrentPosAsVisited();
                break;
            case SOUTH:
                currentRow += 1;
                markCurrentPosAsVisited();
                break;
            case EAST:
                currentCol += 1;
                markCurrentPosAsVisited();
                break;
            case WEST:
                currentCol -= 1;
                markCurrentPosAsVisited();
                break;
            default:
                break;
        }
    }

    public void rotateLeft() {
        turn(false);
    }

    public void rotateRight() {
        turn(true);
    }

    public void rotate180() {
        turn(true);
        turn(true);
    }

//==========================================================Function for Exploration==========================================================================
    /**
     * For getting the robot's X and Y coordinates relative the the map
     */
    public int getCurrentCol() {
        return currentCol;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * Returns the current direction that the robot is facing
     */
    public DIRECTION getDirection() {
        return direction;
    }

    public void markStartAsExplored() {
        int robotMapCol = this.getCurrentCol();
        int robotMapRow = this.getCurrentRow();
        System.out.println();

        Grid[][] robotMapGrids = _robotMap.getMapGrids();
        for (int mapRow = robotMapRow; mapRow < robotMapRow
                + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = robotMapCol; mapCol < robotMapCol
                    + RobotConstant.ROBOT_SIZE; mapCol++) {

                robotMapGrids[mapRow][mapCol].setExplored(true);
            }
        }
    }

    private void markCurrentPosAsVisited() {
        Grid[][] robotMapGrids = _robotMap.getMapGrids();

        for (int mapRow = currentRow; mapRow < currentRow
                + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = currentCol; mapCol < currentCol
                    + RobotConstant.ROBOT_SIZE; mapCol++) {
                robotMapGrids[mapRow][mapCol].markAsVisited();
            }
        }
    }

    public ArrayList<Sensor> simulateSensors(int robotStartRow, int robotStartCol, DIRECTION robotDirection) {

        _sensors = new ArrayList<Sensor>();

        Sensor _frontLeftSensor = null;
        Sensor _frontRightSensor = null;
        Sensor _frontSensor = null;
        Sensor _leftFrontSensor = null;
        Sensor _leftMidSensor = null;
        Sensor _rightSensor = null;

        switch (robotDirection) {
            case NORTH:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.NORTH);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.NORTH);
                _frontSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow, robotStartCol + 1, DIRECTION.NORTH);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, 4, robotStartRow, robotStartCol, DIRECTION.WEST);
                _leftMidSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 1, robotStartCol, DIRECTION.WEST);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 1, robotStartCol + 2, DIRECTION.EAST);

                _sensors.add(_frontLeftSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftMidSensor);
                _sensors.add(_rightSensor);
                break;
            case WEST:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.WEST);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.WEST);
                _frontSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 1, robotStartCol, DIRECTION.WEST);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, 4, robotStartRow + 2, robotStartCol, DIRECTION.SOUTH);
                _leftMidSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 1, DIRECTION.SOUTH);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow, robotStartCol + 1, DIRECTION.NORTH);

                _sensors.add(_frontLeftSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftMidSensor);
                _sensors.add(_rightSensor);
                break;
            case EAST:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.EAST);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.EAST);
                _frontSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 1, robotStartCol + 2, DIRECTION.EAST);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, 4, robotStartRow, robotStartCol + 2, DIRECTION.NORTH);
                _leftMidSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 1, DIRECTION.NORTH);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 2, robotStartCol + 1, DIRECTION.SOUTH);

                _sensors.add(_frontLeftSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftMidSensor);
                _sensors.add(_rightSensor);
                break;
            case SOUTH:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.SOUTH);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.SOUTH);
                _frontSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 2, robotStartCol + 1, DIRECTION.SOUTH);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, 4, robotStartRow + 2, robotStartCol + 2, DIRECTION.EAST);
                _leftMidSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 1, robotStartCol + 2, DIRECTION.EAST);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 1, robotStartCol, DIRECTION.WEST);

                _sensors.add(_frontLeftSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftMidSensor);
                _sensors.add(_rightSensor);
                break;
            default:
        }
        return _sensors;
    }

//==========================================================================For A* Star Search========================================================================
    public boolean isFrontNeighbour(int currentIndex, int[] mapBit) {
        int frontNeighbour;

        frontNeighbour = currentIndex - 15;

        if (frontNeighbour < 0) {
            return false;
        } else {
            for (int i = frontNeighbour; i < (frontNeighbour + RobotConstant.ROBOT_SIZE); i++) {
                if (frontNeighbour < 300 && mapBit[i] == 1) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isLeftNeighbour(int currentIndex, int[] mapBit) {
        int leftNeighbour;

        leftNeighbour = currentIndex - 1;

        if (leftNeighbour % 15 == 14 || (leftNeighbour + 30) > 299 || leftNeighbour < 0) {
            return false;
        } else {
            for (int i = leftNeighbour; i <= (leftNeighbour + 30); i += 15) {
                if (leftNeighbour < 300 && mapBit[i] == 1) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isRightNeighbour(int currentIndex, int[] mapBit) {

        int rightNeighbour;
        rightNeighbour = currentIndex + RobotConstant.ROBOT_SIZE;

        if ((rightNeighbour) % 15 == 0 || (rightNeighbour + 30) > 299 || rightNeighbour > 299) {
            return false;
        } else {
            for (int i = rightNeighbour; i <= (rightNeighbour + 30); i += 15) {
                if (rightNeighbour < 300 && mapBit[i] == 1) {
                    return false;
                }
            }
            return true;

        }
    }

    public boolean isBottomNeighbour(int currentIndex, int[] mapBit) {

        int bottomNeighbour = currentIndex + 45;

        if (bottomNeighbour > 297 || (bottomNeighbour + 2) > 299) //Need check
        {
            return false;
        } else {
            for (int i = bottomNeighbour; i < (bottomNeighbour + RobotConstant.ROBOT_SIZE); i++) {
                if (bottomNeighbour < 300 && mapBit[i] == 1) {
                    return false;
                }
            }
            return true;
        }
    }

    public ArrayList<Integer> currentNeighbour(int currentIndex, DIRECTION direction, int[] mapBit) {
        ArrayList<Integer> currentNeighbour = new ArrayList<Integer>();
        if (isFrontNeighbour(currentIndex, mapBit)) {
            currentNeighbour.add(currentIndex - 15);
        }
        if (isLeftNeighbour(currentIndex, mapBit)) {
            currentNeighbour.add(currentIndex - 1);
        }
        if (isRightNeighbour(currentIndex, mapBit)) {
            currentNeighbour.add(currentIndex + 1);
        }
        if (isBottomNeighbour(currentIndex, mapBit)) {
            currentNeighbour.add(currentIndex + 15);
        }

        return currentNeighbour;
    }

    public boolean checkNeighbour(int neighbour, ArrayList<Integer> openSet) {
        for (int i = 0; i < openSet.size(); i++) {
            if (openSet.get(i) == neighbour) {
                return true;
            }
        }
        return false;
    }

    public double heuristicDist(int start, int end) {                                                 //Function for calculation of Heuristic Distance
        int startX = ConvertCol(start);
        int startY = ConvertRow(start);
        int endX = ConvertCol(end);
        int endY = ConvertRow(end);

        return Math.abs(startX - endX) + Math.abs(startY - endY);
    }

    public int minimumScore(double[] fScore, ArrayList<Integer> openSet) {
        double lowestfScore = 9999;
        int lowestfIndex = -1;
        for (int i = 0; i < openSet.size(); i++) {
            if (fScore[openSet.get(i)] <= lowestfScore) {
                lowestfScore = fScore[openSet.get(i)];
                lowestfIndex = i;
            }
        }
        return lowestfIndex;
    }

    public int decisionStart(double[] fScore, ArrayList<Integer> openSet, DIRECTION direction, int start) {
        double lowestfScore = 9999;
        System.out.println(direction);
        System.out.println(start);
        ArrayList<Integer> decisionStart = new ArrayList<Integer>();
        int j = 0;
        for (int i = 0; i < openSet.size(); i++) {
            if (fScore[openSet.get(i)] <= lowestfScore) {
                lowestfScore = fScore[openSet.get(i)];
                decisionStart.add(i);
            }
        }
        for (j = 0; j < decisionStart.size(); j++) {
            switch (direction) {
                case NORTH:
                    if ((start - 15) == openSet.get(j)) {
                        return decisionStart.get(j);
                    }
                case EAST:
                    if ((start + 1) == openSet.get(j)) {
                        return decisionStart.get(j);
                    }
                case WEST:
                    if ((start - 1) == openSet.get(j)) {
                        return decisionStart.get(j);
                    }
                case SOUTH:
                    if ((start + 15) == openSet.get(j)) {
                        return decisionStart.get(j);
                    }
            }
        }
        return j;
    }

    public ArrayList<Integer> shortestPathResult(int[] cameFrom, int currentIndex, int start) { //Added start
        ArrayList<Integer> path = new ArrayList();
        while (cameFrom[currentIndex] != -1) {
            path.add(currentIndex);
            currentIndex = cameFrom[currentIndex];

        }
        if (start == ((RobotConstant.DEFAULT_START_ROW * 15) + RobotConstant.DEFAULT_START_COL)) {
            path.add(start);
        }
        return path;
    }

    public boolean PrintShortestPath(int index, ArrayList<Integer> shortestPath) {
        for (int i = 0; i < shortestPath.size(); i++) {
            if (index == shortestPath.get(i)) {
                return true;
            }
        }
        return false;
    }

    public String hexToBin(String hex) {                                                              //Function for Hexadecimal to Binary conversion
        int i = Integer.parseInt(hex, 16);
        String bin = Integer.toBinaryString(i);
        return bin;
    }

    public DIRECTION directionPrinting(int currentIndex, ArrayList<Integer> shortestPath) {
        int i;
        int direction;
        for (i = 0; i < shortestPath.size(); i++) {
            if (currentIndex == shortestPath.get(i)) {
                if (i == 0) {
                    return DIRECTION.NORTH;
                }
                direction = shortestPath.get(i - 1) - currentIndex;
                if (direction == 15) {
                    return DIRECTION.NORTH;
                } else if (direction == -1) {
                    return DIRECTION.EAST;
                } else if (direction == 1) {
                    return DIRECTION.WEST;
                } else if (direction == -15) {
                    return DIRECTION.SOUTH;
                }
            }
        }
        return null;
    }

    public DIRECTION directionIndicator(int oldCurrentIndex, int newCurrentIndex, DIRECTION direction) {
        if (oldCurrentIndex - newCurrentIndex == 15) {
            return DIRECTION.NORTH;
        } else if (oldCurrentIndex - newCurrentIndex == -1) {
            return DIRECTION.EAST;
        } else if (oldCurrentIndex - newCurrentIndex == 1) {
            return DIRECTION.WEST;
        } else if (oldCurrentIndex - newCurrentIndex == -15) {
            return DIRECTION.SOUTH;
        } else if ((oldCurrentIndex - newCurrentIndex == 0)) {
            return direction;
        }
        return null;
    }

//    public ArrayList<String> sendSerialMovement(ArrayList<Integer> shortestPathResult) {
//        System.out.println("Size: " + shortestPathResult.size());
//        System.out.println("Checking Array: " + shortestPathResult);
//        ArrayList<String> movementArrayList = new ArrayList<String>();
//        String previous = "";
//        movementArrayList.add("W");
////         movementArrayList.add(shortestPathResult.get(0));
//        for (int i = 0; i < shortestPathResult.size() - 1; i++) {
//            if (shortestPathResult.get(i) - shortestPathResult.get(i + 1) == -1 && previous != "East") {
//                previous = "East";
//                movementArrayList.add("W");
//            } else if (shortestPathResult.get(i) - shortestPathResult.get(i + 1) == 1 && previous != "West") {
//                previous = "West";
//                movementArrayList.add("W");
//            } else if (shortestPathResult.get(i) - shortestPathResult.get(i + 1) == -15 && previous != "South") {
//                previous = "South";
//                movementArrayList.add("W");
//            } else if (shortestPathResult.get(i) - shortestPathResult.get(i + 1) == 15 && previous != "North") {
//                previous = "North";
//                movementArrayList.add("W");
//            }
//        }
//        return movementArrayList;
//    }
    public ArrayList<String> sendSerialMovementSteps(ArrayList<Integer> shortestPathResult) {
        int previous = shortestPathResult.get(0);
        int previousDiff = shortestPathResult.get(0) - shortestPathResult.get(1);
        ArrayList<String> stepsArrayList = new ArrayList<String>();
        int counter = 0;

        for (int i = 1; i < shortestPathResult.size(); i++) {
            int diff = previous - shortestPathResult.get(i);
            previous = shortestPathResult.get(i);
            if (diff == previousDiff) {
                previousDiff = diff;
                counter++;
            } else {
                stepsArrayList.add(Integer.toString(counter));
                stepsArrayList.add("1");
                counter = 1;
                previousDiff = diff;
            }
        }
        stepsArrayList.add(Integer.toString(counter));
        return stepsArrayList;
    }

    public ArrayList<String> sendSerialMovement(ArrayList<Integer> shortestPathResult) {
        int previous = shortestPathResult.get(0);
        int previousDiff = shortestPathResult.get(0) - shortestPathResult.get(1);
        ArrayList<String> movementArrayList = new ArrayList<String>();
        
        movementArrayList.add("W");
        for (int i = 1; i < shortestPathResult.size(); i++) {
            int diff = previous - shortestPathResult.get(i);
            previous = shortestPathResult.get(i);
            if (diff == previousDiff) {
                previousDiff = diff;
            } else {
                if (previousDiff == -1 && diff == 15) {
                    movementArrayList.add("A");
                } else if (previousDiff == -1 && diff == -15) {
                    movementArrayList.add("D");
                } else if (previousDiff == 1 && diff == 15) {
                    movementArrayList.add("D");
                } else if (previousDiff == 1 && diff == -15) {
                    movementArrayList.add("A");
                } else if (previousDiff == 15 && diff == 1) {
                    movementArrayList.add("A");
                } else if (previousDiff == 15 && diff == -1) {
                    movementArrayList.add("D");
                } else if (previousDiff == -15 && diff == 1) {
                    movementArrayList.add("D");
                } else if (previousDiff == -15 && diff == -1) {
                    movementArrayList.add("A");
                } else {
                    movementArrayList.add("S");
                }
                movementArrayList.add("W");
                previousDiff = diff;
            }
        }
        return movementArrayList;
    }

    public ArrayList<Integer> aStarSearch(int start, int goal, int[] mapBit, DIRECTION direction) {                        //Function of A* search algorithm
        boolean closedSet[] = new boolean[300];                                                             //The set of nodes already evaluated
        ArrayList<Integer> openSet = new ArrayList();                                                       //The set of currently discovered nodes that are not evaluated yet
        int cameFrom[] = new int[300];                                                                      //CameFrom will eventually contain the most efficient previous step
        double gScore[] = new double[300];                                                                  //For each node, the cost of getting from the start node to that node.
        double fScore[] = new double[300];                                                                  //For each node, the total cost of getting from the start node to the goal
        DIRECTION cameFromDirection[] = new DIRECTION[300];
        double gScoreTemp;
        int lowestIndex = 0;
        ArrayList<Integer> neighbour;
        int currentIndex = start;
        DIRECTION starDirection = direction;
        ArrayList Catch = new ArrayList<Integer>();
//        int decisionStartFlag = 0;

        Arrays.fill(closedSet, false);
        Arrays.fill(cameFrom, -1);
        Arrays.fill(gScore, 999);
        Arrays.fill(fScore, 999);

        openSet.add(start);
        gScore[start] = 0;
        fScore[start] = heuristicDist(start, goal);

        while (openSet.size() != 0) {
//            if (decisionStartFlag == 1) {
//                System.out.println("decisionStartFlag: " + decisionStartFlag);
//                lowestIndex = decisionStart(fScore, openSet, starDirection, start);
//                decisionStartFlag++;
//            } else{
            lowestIndex = minimumScore(fScore, openSet);
//                decisionStartFlag++;
//            }
            starDirection = directionIndicator(currentIndex, openSet.get(lowestIndex), starDirection);
            currentIndex = openSet.get(lowestIndex);
            openSet.remove(lowestIndex);
            System.out.println(Catch);
            if (currentIndex == goal) {
                midDirection = starDirection;
                return shortestPathResult(cameFrom, currentIndex, start);
            }
            closedSet[currentIndex] = true;
            neighbour = currentNeighbour(currentIndex, direction, mapBit);

            for (int i = 0; i < neighbour.size(); i++) {
                if (closedSet[neighbour.get(i)]) {
                    continue;
                }
                if (checkNeighbour(neighbour.get(i), openSet) == false) {
                    openSet.add(neighbour.get(i));
                }
                gScoreTemp = gScore[currentIndex] + 1;
                if (gScoreTemp >= gScore[neighbour.get(i)]) {
                    continue;
                } else if (starDirection == directionIndicator(currentIndex, neighbour.get(i), starDirection)) {
//                    System.out.println("starDirection: " + starDirection + " directionIndicator: " + directionIndicator(currentIndex, neighbour.get(i), starDirection));
//                    System.out.println("true, Neighbour: " + neighbour.get(i) + " currentIndex: " + currentIndex);
                    cameFromDirection[neighbour.get(i)] = directionIndicator(currentIndex, neighbour.get(i), starDirection);
                    cameFrom[neighbour.get(i)] = currentIndex;
                    gScore[neighbour.get(i)] = gScoreTemp;
                    fScore[neighbour.get(i)] = gScore[neighbour.get(i)] + heuristicDist(neighbour.get(i), goal);
                } else {
//                    System.out.println("starDirection: " + starDirection + " directionIndicator: " + directionIndicator(currentIndex, neighbour.get(i), starDirection));
//                    System.out.println("false, Neighbour: " + neighbour.get(i) + " currentIndex: " + currentIndex);
                    cameFromDirection[neighbour.get(i)] = directionIndicator(currentIndex, neighbour.get(i), starDirection);
                    cameFrom[neighbour.get(i)] = currentIndex;
                    gScore[neighbour.get(i)] = gScoreTemp;
                    fScore[neighbour.get(i)] = gScore[neighbour.get(i)] + heuristicDist(neighbour.get(i), goal) + 10;
                }
            }
        }
        return null;        //No Solution
    }

    public void startShortestPath(String exploredMap) {
//        String map = "C02080000000000000000000001FF1C000000000010FFC000000000000000000000000010103";        //Hardcoded Map in Hexadecimal
        String map = exploredMap;
        int mapBit[] = new int[300];                                                                        //Map in Binary     
        int start = 0;                                                                                     //Start Point
        int end = 267;                                                                                      //Goal Point
        int mid = _mapUI.getMidIndex();                                                                                      //Mid Point
        DIRECTION aDirection = direction;
        String temp;                                                                                        //Temporary storage for 1 Hex to 4 Bit conversion                                                                                    
        String bitString = "";                                                                              //Initialization of map in binary
        ArrayList<Integer> shortestPath = new ArrayList<Integer>();
        ArrayList<Integer> shortestPath1;                                                                   //Shortest Path Result
        ArrayList<Integer> shortestPath2;                                                                   //Shortest Path from mid to goal point 

        for (int i = 0; i < 76; i++) {
            temp = String.format("%04d", Integer.parseInt(hexToBin(map.substring(i, i + 1))));                //Hexadecimal to Binary conversion
            bitString += temp;
        }

        bitString = bitString.substring(2, bitString.length() - 2);                                           //Remove first 2 bit and last 2 bit of the String  

        for (int h = 0; h < 300; h++) {
            mapBit[h] = Integer.parseInt(bitString.substring(h, h + 1));                                      //String to Int Conversion
        }
//
        if (_mapUI.getMidIndex() == 0 || _mapUI.getMidIndex() == 267) {
            shortestPath = aStarSearch(start, end, mapBit, aDirection);
            Collections.reverse(shortestPath);
        } else {
            shortestPath1 = aStarSearch(start, mid, mapBit, aDirection);
            shortestPath2 = aStarSearch(mid, end, mapBit, midDirection);

            shortestPath.addAll(shortestPath2);                                                                 //concatenate of 2 Arraylist
            shortestPath.addAll(shortestPath1);
            Collections.reverse(shortestPath);
        }
        System.out.println("");

        for (int i = 0; i < 300; i++) {
            System.out.print(" " + String.format("%03d", i) + " ");
            if (i % 15 == 14) {
                System.out.println("");
            }

        }
        System.out.println("");

        for (int i = 0; i < 300; i++) {
            if (i == start) {
                System.out.print(" S ");
            } //            else if(i == mid)
            //                System.out.print(" W ");
            else if (i == end) {
                System.out.print(" E ");
            } else if (PrintShortestPath(i, shortestPath)) {
                switch (directionPrinting(i, shortestPath)) {
                    case NORTH:
                        System.out.print(" ^ ");
                        break;
                    case EAST:
                        System.out.print(" > ");
                        break;
                    case WEST:
                        System.out.print(" < ");
                        break;
                    case SOUTH:
                        System.out.print(" V ");
                        break;
                    default:
                        System.out.println("Error");
                }
            } else if (bitString.charAt(i) == '1') {
                System.out.print(" . ");
            } else if (bitString.charAt(i) == '0') {
                System.out.print(" * ");
            }
            if (i % 15 == 14) {
                System.out.println("");
            }
        }
        for (int i = 0; i < shortestPath.size(); i++) //Printing of shortest path result
        {
            System.out.print(shortestPath.get(i) + " -> ");//@@@@@@@@@@@@@@@@@@@@@@@@ 
        }
        System.out.println("");
        System.out.println("Movements: " + sendSerialMovement(shortestPath));
        System.out.println("Steps: " + sendSerialMovementSteps(shortestPath));
        simulateShortestPath(shortestPath);
    }

    public void simulateShortestPath(ArrayList<Integer> shortestPath) {
        _timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
        System.out.println("Steps Per Second: " + _stepsPerSecond
                + ", Timer Interval: " + _timerIntervals);

        // Reset the elapsed exploration time (in milliseconds)
        _elapsedShortestPathTime = 0;
        _bReachedGoal = false;
        _robotMap.setShortestPath();

        _shortestPathTimer = new Timer(_timerIntervals, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (_shortestPathTimer != null && _bReachedGoal) {
                    _shortestPathTimer.stop();
                    _shortestPathTimer = null;
                } else {
                    //Check if robot is in the goal zone
                    if (withinGoalZone(currentRow, currentCol)) {
                        _bReachedGoal = true;
                    }
                    int gridIndex, nextIndex, gridCol, gridRow, nextCol, nextRow, newCol, newRow, oldCol, oldRow;
                    DIRECTION nextDir;

                    oldCol = currentCol;
                    oldRow = currentRow;
                    // Make the next move
                    if (j < shortestPath.size()) {
                        gridIndex = shortestPath.get(j);
                        gridCol = ConvertCol(gridIndex);
                        gridRow = ConvertRow(gridIndex);
                        if ((j + 1) < shortestPath.size()) {

                            nextIndex = shortestPath.get(j + 1);
                            nextCol = ConvertCol(nextIndex);
                            nextRow = ConvertRow(nextIndex);
                            System.out.println("grid[" + nextRow + "][" + nextCol + "]");
                            if ((nextRow == (gridRow + 1) && nextCol == gridCol)) {
                                nextDir = DIRECTION.SOUTH;
                            } else if ((nextRow == (gridRow - 1) && nextCol == gridCol)) {
                                nextDir = DIRECTION.NORTH;
                            } else if ((nextCol == (gridCol + 1) && nextRow == gridRow)) {
                                nextDir = DIRECTION.EAST;
                            } else {
                                nextDir = DIRECTION.WEST;
                            }

                            if (checkTurnLeft(direction, nextDir)) {
                                rotateLeft();
                                moveForward();
                            } else if (checkTurnRight(direction, nextDir)) {
                                rotateRight();
                                moveForward();
                            } else if (checkTurn180(direction, nextDir)) {
                                rotateRight();
                                rotateRight();
                                moveForward();
                            } else {
                                moveForward();
                            }
                            newCol = currentCol;
                            newRow = currentRow;
                            updatePosition(oldRow, oldCol, newRow, newCol);
                        }
                        j++;
                    }
                    _robotMap.revalidate();
                    _robotMap.repaint();

                    // Update elapsed time
                    _elapsedShortestPathTime += _timerIntervals;
                }
            }
        });
        _shortestPathTimer.setRepeats(true);
        _shortestPathTimer.setInitialDelay(1000);
        _shortestPathTimer.start();
    }

    public boolean checkTurnLeft(DIRECTION curDir, DIRECTION nextDir) {
        switch (curDir) {
            case NORTH:
                if (nextDir == DIRECTION.WEST) {
                    return true;
                }
                break;
            case SOUTH:
                if (nextDir == DIRECTION.EAST) {
                    return true;
                }
                break;
            case EAST:
                if (nextDir == DIRECTION.NORTH) {
                    return true;
                }
                break;
            case WEST:
                if (nextDir == DIRECTION.SOUTH) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    public boolean checkTurnRight(DIRECTION curDir, DIRECTION nextDir) {
        switch (curDir) {
            case NORTH:
                if (nextDir == DIRECTION.EAST) {
                    return true;
                }
                break;
            case SOUTH:
                if (nextDir == DIRECTION.WEST) {
                    return true;
                }
                break;
            case EAST:
                if (nextDir == DIRECTION.SOUTH) {
                    return true;
                }
                break;
            case WEST:
                if (nextDir == DIRECTION.NORTH) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    public boolean checkTurn180(DIRECTION curDir, DIRECTION nextDir) {
        switch (curDir) {
            case NORTH:
                if (nextDir == DIRECTION.SOUTH) {
                    return true;
                }
                break;
            case SOUTH:
                if (nextDir == DIRECTION.NORTH) {
                    return true;
                }
                break;
            case EAST:
                if (nextDir == DIRECTION.WEST) {
                    return true;
                }
                break;
            case WEST:
                if (nextDir == DIRECTION.EAST) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

}
