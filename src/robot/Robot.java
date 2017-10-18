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
import comms.CommsMgr;

public class Robot {

    CommsMgr mgr = null;
    int testCount = 0;

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

    //Physical exploration variables
    private transient Timer _phyExploreTimer = null;
    private transient boolean _bPhyExConnected = false;
    private transient int _phyExErrors = 0;
    private transient String _phyExRcvMsg = null;
    private transient boolean _bPhyExStarted = false;
    private transient static final String START_PHY_EXPLORE = "e";
    private transient String _phyExCmdMsg = null;
    private transient String _phyExSimMsg = "";
    private transient static final int MAX_MOVES_BEFORE_CALIBRATION = 6;
    private transient int _movesSinceLastCalibration = MAX_MOVES_BEFORE_CALIBRATION;

    // For physical shortest path
    private transient Timer _phySpTimer = null;
    private transient boolean _bPhySpConnected = false;
    private transient int _phySpErrors = 0;
    private transient String _phySpRcvMsg = null;
    private transient boolean _bPhySpStarted = false;
    private transient static final String START_PHY_SP = "f";
    private transient String _phySpCmdMsg = null;

    //Exploring Unexplored
    private transient boolean _bExploreUnexplored = false;
    private transient boolean _bExploring = false;
    ArrayList<Integer> exploreUnexploredPath;
    private boolean _exploreUnexploredFlag = false;
    int targetGrid = 0;
    private transient boolean movingToStart = false;

    private DIRECTION midDirection;
    int j = 0;
    //int k = 0;
    int startCalibration = 1;
    int validateCount = 0;
    int totalRotation = 0;
    int forwardCount = 0;
    private boolean isStairs = false;
    int stepCount = 0;

    private ArrayList<Integer> _unexploredGrids = null;
    private ArrayList<Integer> _unreachableGrids = new ArrayList<Integer>();

    /**
     * Gets the list of the robot's sensors
     *
     * @return The list of sensors on the robot
     */
    public ArrayList<Sensor> getSensors() {
        return _sensors;
    }

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
        this.updatePosition(startMapPosRow, startMapPosCol);

        // Reset variables used for exploration
        _bReachedGoal = false;
        _bExplorationComplete = false;
        _bPreviousLeftWall = false;
        if(mgr!=null){
            mgr.closeConnection();
            mgr = null;
        }
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
        _bExploreUnexplored = false;
        _bExploring = false;
        targetGrid = 0;
        movingToStart = false;

        j = 0;
        validateCount = 0;
        totalRotation = 0;
        startCalibration = 1;

    }

    public void makeNextMove() {
        // Sense its surroundings

        _robotMap.revalidate();
        _robotMap.repaint();

        // Logic to make the next move
        //this.logic();
        this.PossibleMove();
        this.sense();

    }

    public void sense() {

        // Weightage of the sensors
        double[] sensorWeightage = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

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
                            //System.out.println("sensor row: "+gridRow+" col:"+gridCol);
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
                int startingGrid = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);
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
                int startingGrid = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);
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

        if (_bExploreUnexplored) {
            int currentGrid = ConvertToIndex(currentRow, currentCol);
            //System.out.println("unreachable: " + _unreachableGrids);
            _unexploredGrids = getUnexploredGrids();
//            System.out.println("unexplored: " + _unexploredGrids);
            if (_unexploredGrids.isEmpty() && currentGrid != ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                moveToStart();
                //_bExploreUnexplored = false;
                return;
            } else if (_unexploredGrids.isEmpty() && currentGrid == ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                _bExploreUnexplored = false;
                return;
            }

            if (!_bExploring) {
                double targetDist = 0;
                double tempDist = 0;
                int reachableGrid = 0;
                int startGrid = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);

                for (int i = 0; i < _unexploredGrids.size(); i++) {
                    tempDist = heuristicDist(startGrid, _unexploredGrids.get(i));
                    if (tempDist > targetDist) {
                        targetGrid = _unexploredGrids.get(i);
                        targetDist = tempDist;
                    }
                }
                reachableGrid = findReachableGrid(targetGrid);
                //System.out.println("target grid: "+targetGrid);
                //System.out.println("reachable grid: "+reachableGrid);
                if (reachableGrid != 0) {
                    exploreUnexploredPath = generatePath(currentGrid, reachableGrid); //generate Path
                    if (exploreUnexploredPath == null) {
                        blockAddUnreachable(targetGrid, reachableGrid);
                        //System.out.println("why" + _unreachableGrids);
                        //_unreachableGrids.add(targetGrid);
                    } else {
                        if (currentGrid != ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                            exploreUnexploredPath.add(currentGrid);
                        }
                        Collections.reverse(exploreUnexploredPath);
                        if (exploreUnexploredPath.size() >= 1) {
                            _bExploring = true;
                        }
                    }
                } else if (!_unreachableGrids.contains(targetGrid)) {
                    _unreachableGrids.add(targetGrid);
                }
            } else {
                exploreUnexplored();
            }
            //System.out.println("test");
            return;
        }

        if (_bReachedGoal && withinStartZone(currentRow, currentCol)) {
            _unexploredGrids = getUnexploredGrids();
            if (!_unexploredGrids.isEmpty()) {
                _bExploreUnexplored = true;
                while (direction != RobotConstant.DEFAULT_START_DIR) {
                    rotateRight();
                }
            } else {
                _bExplorationComplete = true;
                System.out.println("MDF String part 1:" + _robotMap.generateMDFStringPart1());
                System.out.println("MDF String part 2:" + _robotMap.generateMDFStringPart2());
                System.out.println("MDF String:" + _mapUI.generateMapString());
            }
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
        }

        // Save current leftWall state into _bPreviousLeftWall
        _bPreviousLeftWall = leftWall;
    }

    public void blockAddUnreachable(int target, int limit) {
        int addIndex, targetRow, targetCol, limitRow, limitCol, rowDistance, colDistance, row, col;
        targetRow = ConvertRow(target);
        targetCol = ConvertCol(target);
        limitRow = ConvertRow(limit);
        limitCol = ConvertCol(limit);

        rowDistance = limitRow - targetRow; // 2
        colDistance = limitCol - targetCol;//-1
        //System.out.println("target: " +target+" limit: "+ limit);
        if (rowDistance < 0) {
            if (colDistance < 0) {
                for (row = limitRow; row <= targetRow; row++) {
                    for (col = limitCol; col <= targetCol; col++) {
                        addIndex = ConvertToIndex(row, col);
                        if (!_unreachableGrids.contains(addIndex)) {
                            _unreachableGrids.add(addIndex);
                        }
                    }
                }
            } else {
                for (row = limitRow; row <= targetRow; row++) {
                    for (col = targetCol; col <= limitCol; col++) {
                        addIndex = ConvertToIndex(row, col);
                        if (!_unreachableGrids.contains(addIndex)) {
                            _unreachableGrids.add(addIndex);
                        }
                    }
                }
            }
        } else if (colDistance < 0) {
            for (row = targetRow; row <= limitRow; row++) {
                for (col = limitCol; col <= targetCol; col++) {
                    addIndex = ConvertToIndex(row, col);
                    if (!_unreachableGrids.contains(addIndex)) {
                        _unreachableGrids.add(addIndex);
                    }
                }
            }
        } else {
            for (row = targetRow; row <= limitRow; row++) {
                for (col = targetCol; col <= limitCol; col++) {
                    addIndex = ConvertToIndex(row, col);
                    if (!_unreachableGrids.contains(addIndex)) {
                        _unreachableGrids.add(addIndex);
                    }
                }
            }
        }

//        for(int row = limitRow; row < limitRow + rowDistance; row++){
//            for(int col = limitCol; col < limitCol +colDistance; col++)
//                _unreachableGrids.add(ConvertToIndex(row, col));
//        }
    }

    public ArrayList<Integer> generatePath(int current, int reachable) {
        String mapString = "";
        int mapBit[] = new int[300];
        Grid[][] _grids = _robotMap.getMapGrids();

        for (int row = 0; row < Constants.MAP_ROWS; row++) {
            for (int col = 0; col < Constants.MAP_COLS; col++) {
                if (_grids[row][col].isExplored()) {
                    mapString += _grids[row][col].isObstacle() ? "1" : "0";                                 //Generate Map String
                } else {
                    mapString += 0;
                }
            }
        }
        for (int h = 0; h < 300; h++) {
            mapBit[h] = Integer.parseInt(mapString.substring(h, h + 1));                                      //String to Int Conversion
        }

        return aStarSearch(current, reachable, mapBit, direction);
    }

    public void moveToStart() {
        if (!movingToStart) {
            int currentGrid = ConvertToIndex(currentRow, currentCol);
            int startGrid = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);
            String mapString = "";
            int mapBit[] = new int[300];
            Grid[][] _grids = _robotMap.getMapGrids();

            for (int row = 0; row < Constants.MAP_ROWS; row++) {
                for (int col = 0; col < Constants.MAP_COLS; col++) {
                    if (_grids[row][col].isExplored()) {
                        mapString += _grids[row][col].isObstacle() ? "1" : "0";                                 //Generate Map String
                    } else {
                        mapString += 0;
                    }
                }
            }
            for (int h = 0; h < 300; h++) {
                mapBit[h] = Integer.parseInt(mapString.substring(h, h + 1));                                      //String to Int Conversion
            }

            exploreUnexploredPath = aStarSearch(currentGrid, startGrid, mapBit, direction); //generate Path
            exploreUnexploredPath.add(currentGrid);
            Collections.reverse(exploreUnexploredPath);
            movingToStart = true;
            _exploreUnexploredFlag = true;
        } else {
            int gridRow, gridCol, nextRow, nextCol;
            DIRECTION nextDir;
            if(_exploreUnexploredFlag == true){
                _exploreUnexploredFlag = false;
            }
            if (!exploreUnexploredPath.isEmpty()) {
                //System.out.println("before: "+exploreUnexploredPath.get(0));
                gridCol = ConvertCol(exploreUnexploredPath.get(0));
                gridRow = ConvertRow(exploreUnexploredPath.get(0));
                if (exploreUnexploredPath.size() > 1) {
                    nextCol = ConvertCol(exploreUnexploredPath.get(1));
                    nextRow = ConvertRow(exploreUnexploredPath.get(1));

                    if (checkForObstacles(nextRow, nextCol)) {
                        exploreUnexploredPath.clear();
                        return;
                    }

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
                        _phyExCmdMsg = "Q";
                    } else if (checkTurnRight(direction, nextDir)) {
                        rotateRight();
                        moveForward();
                        _phyExCmdMsg = "T";
                    } else if (checkTurn180(direction, nextDir)) {
                        rotateRight();
                        rotateRight();
                        moveForward();
                        _phyExCmdMsg = "X";
                    } else {
                        moveForward();
                        _phyExCmdMsg = "W";
                    }
                    exploreUnexploredPath.remove(0);
                    if(exploreUnexploredPath.isEmpty()){
                        _exploreUnexploredFlag = true;
                    }
                    if (_bPhyExStarted == true && _phyExCmdMsg != null) {
                        String outputMsg = _phyExCmdMsg;
                        mgr.sendMsg(outputMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
                        mgr.sendMsg(outputMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                        _phyExCmdMsg = null;
                    }
                    //System.out.println("after: "+exploreUnexploredPath.get(0));
                }
            }
        }
    }

    public void exploreUnexplored() {
        int gridRow, gridCol, nextRow, nextCol;
        DIRECTION nextDir;
        System.out.println("executing exploreUnexplored");
        _exploreUnexploredFlag = false;
       
        // Make the next move
        if (!exploreUnexploredPath.isEmpty()) {
            //System.out.println("before: "+exploreUnexploredPath.get(0));
            gridCol = ConvertCol(exploreUnexploredPath.get(0));
            gridRow = ConvertRow(exploreUnexploredPath.get(0));
            if (exploreUnexploredPath.size() > 1) {
                nextCol = ConvertCol(exploreUnexploredPath.get(1));
                nextRow = ConvertRow(exploreUnexploredPath.get(1));
                if (checkForObstacles(nextRow, nextCol)) {
                    exploreUnexploredPath.clear();
                    _exploreUnexploredFlag = true;
                    return;
                }

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
                    _phyExCmdMsg = "A";
                } else if (checkTurnRight(direction, nextDir)) {
                    rotateRight();
                    _phyExCmdMsg = "D";
                } else if (checkTurn180(direction, nextDir)) {
                    rotate180();
                    _phyExCmdMsg = "S";
                } else {
                    moveForward();
                    _phyExCmdMsg = "W";
                    exploreUnexploredPath.remove(0);
                }

                
                //System.out.println("after: "+exploreUnexploredPath.get(0));
            } else if (exploreUnexploredPath.size() == 1) {
                Grid[][] map = _robotMap.getMapGrids();
                if (!map[ConvertRow(_unexploredGrids.get(0))][ConvertCol(_unexploredGrids.get(0))].isExplored()) {
                    if ((isRightWall()) && (!isFrontWall())) {
                        moveForward();
                        _phyExCmdMsg = "W";
                    } else {
                        rotateRight();
                        _phyExCmdMsg = "D";
                        //requestSensorReadings();
                    }
                }
                
                exploreUnexploredPath.remove(0);
                _bExploring = false;
                _exploreUnexploredFlag = true;
            }
        } else {
            _bExploring = false;
            _exploreUnexploredFlag = true;
        }
    }

    public int findReachableGrid(int target) {

        Grid[][] map = _robotMap.getMapGrids();
        Grid endGrid = null;
        int endingGridRow = ConvertRow(target);
        int endingGridCol = ConvertCol(target);
        
        /*FOR TOP TO BOTTOM*/
//         // Scenario 5: (Row - RobotConstants.ROBOT_SIZE, Col - 1) reachable
//        if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol - 1 >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol - 1)) {
//            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol - 1];
//        } // Scenario 2: (Row - 1, Col - RobotConstants.ROBOT_SIZE) reachable
//        else if ((endingGridRow - 1 >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow - 1, endingGridCol - RobotConstant.ROBOT_SIZE)) {
//            endGrid = map[endingGridRow - 1][endingGridCol - RobotConstant.ROBOT_SIZE];
//        } // Scenario 8: (Row - 1, Col + 1) reachable
//        else if ((endingGridRow - 1 >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow - 1, endingGridCol + 1)) {
//            endGrid = map[endingGridRow - 1][endingGridCol + 1];
//        } // Scenario 11: (Row + 1, Col - 1) reachable
//        else if ((endingGridRow + 1 <= 19 && endingGridCol - 1 >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol - 1)) {
//            endGrid = map[endingGridRow + 1][endingGridCol - 1];
//        }  // Scenario 6: (Row - RobotConstants.ROBOT_SIZE, Col - 2) reachable
//        else if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol - 2 >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol - 2)) {
//            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol - 2];
//        } // Scenario 4: (Row - RobotConstants.ROBOT_SIZE, Col) reachable
//        else if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol)) {
//            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol];
//        } // Scenario 3: (Row - 2, Col - RobotConstants.ROBOT_SIZE) reachable
//        else if ((endingGridRow - 2 >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow - 2, endingGridCol - RobotConstant.ROBOT_SIZE)) {
//            endGrid = map[endingGridRow - 2][endingGridCol - RobotConstant.ROBOT_SIZE];
//        } // Scenario 1: (Row , Col - RobotConstants.ROBOT_SIZE) reachable
//        else if ((endingGridRow >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow, endingGridCol - RobotConstant.ROBOT_SIZE)) {
//            endGrid = map[endingGridRow][endingGridCol - RobotConstant.ROBOT_SIZE];
//        } // Scenario 9: (Row - 2, Col + 1) reachable
//        else if ((endingGridRow - 2 >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow - 2, endingGridCol + 1)) {
//            endGrid = map[endingGridRow - 2][endingGridCol + 1];
//        } // Scenario 7: (Row , Col + 1) reachable
//        else if ((endingGridRow >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow, endingGridCol + 1)) {
//            endGrid = map[endingGridRow][endingGridCol + 1];
//        } // Scenario 12: (Row + 1, Col - 2) reachable
//        else if ((endingGridRow + 1 <= 19 && endingGridCol - 2 >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol - 2)) {
//            endGrid = map[endingGridRow + 1][endingGridCol - 2];
//        } // Scenario 10: (Row + 1, Col ) reachable
//        else if ((endingGridRow + 1 <= 19 && endingGridCol >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol)) {
//            endGrid = map[endingGridRow + 1][endingGridCol];
//        } else {
//            return 0;
//        }
        /*^FOR TOP TO BOTTOM*/
        
        /*FOR BOTTOM TO TOP*/
        // Scenario 10: (Row + 1, Col ) reachable
        if ((endingGridRow + 1 <= 19 && endingGridCol >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol)) {
            endGrid = map[endingGridRow + 1][endingGridCol];
        } // Scenario 11: (Row + 1, Col - 1) reachable
        else if ((endingGridRow + 1 <= 19 && endingGridCol - 1 >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol - 1)) {
            endGrid = map[endingGridRow + 1][endingGridCol - 1];
        } // Scenario 2: (Row - 1, Col - RobotConstants.ROBOT_SIZE) reachable
        else if ((endingGridRow - 1 >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow - 1, endingGridCol - RobotConstant.ROBOT_SIZE)) {
            endGrid = map[endingGridRow - 1][endingGridCol - RobotConstant.ROBOT_SIZE];
        } // Scenario 8: (Row - 1, Col + 1) reachable
        else if ((endingGridRow - 1 >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow - 1, endingGridCol + 1)) {
            endGrid = map[endingGridRow - 1][endingGridCol + 1];
        } // Scenario 5: (Row - RobotConstants.ROBOT_SIZE, Col - 1) reachable
        else if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol - 1 >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol - 1)) {
            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol - 1];
        } // Scenario 12: (Row + 1, Col - 2) reachable
        else if ((endingGridRow + 1 <= 19 && endingGridCol - 2 >= 0) && !checkForObstacles(endingGridRow + 1, endingGridCol - 2)) {
            endGrid = map[endingGridRow + 1][endingGridCol - 2];
        }  // Scenario 3: (Row - 2, Col - RobotConstants.ROBOT_SIZE) reachable
        else if ((endingGridRow - 2 >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow - 2, endingGridCol - RobotConstant.ROBOT_SIZE)) {
            endGrid = map[endingGridRow - 2][endingGridCol - RobotConstant.ROBOT_SIZE];
        } // Scenario 1: (Row , Col - RobotConstants.ROBOT_SIZE) reachable
        else if ((endingGridRow >= 0 && endingGridCol - RobotConstant.ROBOT_SIZE >= 0) && !checkForObstacles(endingGridRow, endingGridCol - RobotConstant.ROBOT_SIZE)) {
            endGrid = map[endingGridRow][endingGridCol - RobotConstant.ROBOT_SIZE];
        } // Scenario 9: (Row - 2, Col + 1) reachable
        else if ((endingGridRow - 2 >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow - 2, endingGridCol + 1)) {
            endGrid = map[endingGridRow - 2][endingGridCol + 1];
        } // Scenario 7: (Row , Col + 1) reachable
        else if ((endingGridRow >= 0 && endingGridCol + 1 <= 14) && !checkForObstacles(endingGridRow, endingGridCol + 1)) {
            endGrid = map[endingGridRow][endingGridCol + 1];
        } // Scenario 6: (Row - RobotConstants.ROBOT_SIZE, Col - 2) reachable
        else if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol - 2 >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol - 2)) {
            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol - 2];
        } // Scenario 4: (Row - RobotConstants.ROBOT_SIZE, Col) reachable
        else if ((endingGridRow - RobotConstant.ROBOT_SIZE >= 0 && endingGridCol >= 0) && !checkForObstacles(endingGridRow - RobotConstant.ROBOT_SIZE, endingGridCol)) {
            endGrid = map[endingGridRow - RobotConstant.ROBOT_SIZE][endingGridCol];
        }
        else {
            return 0;
        }
        
        /*FOR BOTTOM TO TOP*/
        return ConvertToIndex(endGrid.getRow(), endGrid.getCol());
    }

    private boolean checkForObstacles(int robotPosRow, int robotPosCol) {

        // Check for obstacles within robot's new position
        Grid[][] robotMapGrids = _robotMap.getMapGrids();
//        if ((robotPosRow >= 0 && robotPosRow < Constants.MAP_ROWS) && (robotPosCol >= 0 && robotPosCol < Constants.MAP_COLS)) {
        for (int mapRow = robotPosRow; mapRow < robotPosRow + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = robotPosCol; mapCol < robotPosCol + RobotConstant.ROBOT_SIZE; mapCol++) {
                if ((mapRow >= 0 && mapRow < Constants.MAP_ROWS) && (mapCol >= 0 && mapCol < Constants.MAP_COLS)) {
                    if (robotMapGrids[mapRow][mapCol].isExplored() && robotMapGrids[mapRow][mapCol].isObstacle()) {
                        //System.out.println("obstacle at row: "+mapRow+" col: "+mapCol);
                        return true;
                    }
                }
            }
        }
//        }
        return false;
    }

    private void updatePosition(int newRow, int newCol) {

        // Determine the change in row/column of the robot
        // Determine the change in row/column of the robot
        int deltaRow = newRow - currentRow;
        int deltaCol = newCol - currentCol;
        //System.out.println("new row: " + newRow + " new col: " + newCol);

        // Update the path in the robot map
        RobotMap.PathGrid[][] pathGrids = null;
        if (_robotMap != null) {
            pathGrids = _robotMap.getPathGrids();
        }
        if (pathGrids != null) {
            switch (direction) {
                case EAST:
                    pathGrids[currentRow][currentCol].cE = true;
                    pathGrids[newRow][newCol].cW = true;
                    break;
                case NORTH:
                    pathGrids[currentRow][currentCol].cN = true;
                    pathGrids[newRow][newCol].cS = true;
                    break;
                case SOUTH:
                    pathGrids[currentRow][currentCol].cS = true;
                    pathGrids[newRow][newCol].cN = true;
                    break;
                case WEST:
                    pathGrids[currentRow][currentCol].cW = true;
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

    private ArrayList<Integer> getUnexploredGrids() {
        ArrayList<Integer> unexploredGrids = new ArrayList<Integer>();

        Grid[][] map = _robotMap.getMapGrids();
        for (int i = 0; i < Constants.MAP_ROWS; i++) {
            for (int j = 0; j < Constants.MAP_COLS; j++) {
                if (!map[i][j].isExplored()) {
                    if (!_unreachableGrids.contains(ConvertToIndex(i, j))) {
                        unexploredGrids.add(ConvertToIndex(i, j));
                    }
                }
            }
        }
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
        int newRobotMapPosRow = currentRow;
        int newRobotMapPosCol = currentCol;

        newRobotMapPosRow += (direction == DIRECTION.NORTH) ? -1
                : (direction == DIRECTION.SOUTH) ? 1 : 0;

        newRobotMapPosCol += (direction == DIRECTION.WEST) ? -1
                : (direction == DIRECTION.EAST) ? 1 : 0;

        updatePosition(newRobotMapPosRow, newRobotMapPosCol);

        markCurrentPosAsVisited();
        //_phyExCmdMsg = "W";
    }

    public void rotateLeft() {
        turn(false);
        //_phyExCmdMsg = "A";
    }

    public void rotateRight() {
        turn(true);
        //_phyExCmdMsg = "D";
    }

    public void rotate180() {
        turn(true);
        turn(true);
        //_phyExCmdMsg = "S";
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

        for (int mapRow = currentRow; mapRow < currentRow + RobotConstant.ROBOT_SIZE; mapRow++) {
            for (int mapCol = currentCol; mapCol < currentCol + RobotConstant.ROBOT_SIZE; mapCol++) {
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
        Sensor _leftBackSensor = null;
        Sensor _rightSensor = null;

        switch (robotDirection) {
            case NORTH:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.NORTH);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.NORTH);
                _frontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 1, DIRECTION.NORTH);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.WEST);
                _leftBackSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.WEST);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.EAST);

                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftBackSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_frontLeftSensor);
                _sensors.add(_rightSensor);
                break;
            case WEST:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.WEST);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.WEST);
                _frontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 1, robotStartCol, DIRECTION.WEST);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.SOUTH);
                _leftBackSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.SOUTH);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.NORTH);

                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftBackSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_frontLeftSensor);
                _sensors.add(_rightSensor);
                break;
            case EAST:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.EAST);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.EAST);
                _frontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 1, robotStartCol + 2, DIRECTION.EAST);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.NORTH);
                _leftBackSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol, DIRECTION.NORTH);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.SOUTH);

                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftBackSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_frontLeftSensor);
                _sensors.add(_rightSensor);
                break;
            case SOUTH:
                _frontLeftSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.SOUTH);
                _frontRightSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol, DIRECTION.SOUTH);
                _frontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 1, DIRECTION.SOUTH);
                _leftFrontSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow + 2, robotStartCol + 2, DIRECTION.EAST);
                _leftBackSensor = new Sensor(RobotConstant.SHORT_IR_MIN, RobotConstant.SHORT_IR_MAX, robotStartRow, robotStartCol + 2, DIRECTION.EAST);
                _rightSensor = new Sensor(RobotConstant.LONG_IR_MIN, RobotConstant.LONG_IR_MAX, robotStartRow, robotStartCol, DIRECTION.WEST);

                _sensors.add(_leftFrontSensor);
                _sensors.add(_leftBackSensor);
                _sensors.add(_frontRightSensor);
                _sensors.add(_frontSensor);
                _sensors.add(_frontLeftSensor);
                _sensors.add(_rightSensor);
                break;
            default:
        }
        if (!_bPhyExStarted) {
            this.sense();
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
        ArrayList<Integer> decisionStartWTF = new ArrayList<Integer>();
        int j = 0;
        for (int i = 0; i < openSet.size(); i++) {
            if (fScore[openSet.get(i)] <= lowestfScore) {
                lowestfScore = fScore[openSet.get(i)];
                decisionStartWTF.add(i);
                System.out.println("I have: " + openSet.get(i));
            }
        }
        for (j = 0; j < decisionStartWTF.size(); j++) {
            switch (direction) {
                case NORTH:
                    if ((start - 15) == openSet.get(j)) {
                        return decisionStartWTF.get(j);
                    }
                case EAST:
                    if ((start + 1) == openSet.get(j)) {
                        return decisionStartWTF.get(j);
                    }
                case WEST:
                    if ((start - 1) == openSet.get(j)) {
                        return decisionStartWTF.get(j);
                    }
                case SOUTH:
                    if ((start + 15) == openSet.get(j)) {
                        return decisionStartWTF.get(j);
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

    public String sendSerialMovement(ArrayList<Integer> shortestPathResult) {
        int previous = shortestPathResult.get(0);
        int previousDiff = shortestPathResult.get(0) - shortestPathResult.get(1);
        String movementArrayList = "";
        DIRECTION moveOffRotation = directionIndicator(shortestPathResult.get(0), shortestPathResult.get(1), direction);

//        System.out.println("In sendSerialMovement---");
        System.out.println("moveOffRotation: " + moveOffRotation);
        System.out.println("Real direction: " + direction);
        movementArrayList += "C";
        DIRECTION tempDir = DIRECTION.NORTH;
        while (moveOffRotation != tempDir) {
            tempDir = true ? DIRECTION.getNext(tempDir)
                : DIRECTION.getPrevious(tempDir);
//            System.out.println("Turning.. Now direction is " + direction);
            //System.out.println("Toh");
            //rotateRight();
            movementArrayList += "D";
        }
        

        for (int i = 1; i < shortestPathResult.size(); i++) {
            int diff = previous - shortestPathResult.get(i);
            previous = shortestPathResult.get(i);
            if (diff == previousDiff) {
                previousDiff = diff;
                movementArrayList += "W";
            } else {
                if (previousDiff == -1 && diff == 15) {
                    movementArrayList += "A";
                } else if (previousDiff == -1 && diff == -15) {
                    movementArrayList += "D";
                } else if (previousDiff == 1 && diff == 15) {
                    movementArrayList += "D";
                } else if (previousDiff == 1 && diff == -15) {
                    movementArrayList += "A";
                } else if (previousDiff == 15 && diff == 1) {
                    movementArrayList += "A";
                } else if (previousDiff == 15 && diff == -1) {
                    movementArrayList += "D";
                } else if (previousDiff == -15 && diff == 1) {
                    movementArrayList += "D";
                } else if (previousDiff == -15 && diff == -1) {
                    movementArrayList += "A";
                } else {
                    movementArrayList += "S";
                    totalRotation++;
                }
                totalRotation++;
                movementArrayList += "W";
                previousDiff = diff;
            }
        }
        movementArrayList += "b";
        return movementArrayList;
    }

    public ArrayList<Integer> FastestPath(int start, int goal, int[] mapBit, DIRECTION direction) {
        ArrayList<Integer> neighbour = new ArrayList<Integer>();
        ArrayList<DIRECTION> possibleDirection = new ArrayList<DIRECTION>();
        ArrayList<ArrayList<Integer>> possibleFastestPath = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> compareRotation = new ArrayList<Integer>();
        int lowestNumOfRotate = 9999;
        int lowestNumOfRotateIndex = 0;
        ArrayList<Integer> numOflowest = new ArrayList<Integer>();
        String callforFun = "";

        neighbour = currentNeighbour(start, direction, mapBit);
        System.out.println("The Neighbour: " + neighbour);

        for (int i = 0; i < neighbour.size(); i++) {
            possibleDirection.add(directionIndicator(start, neighbour.get(i), direction));
            if(possibleDirection.get(i) != direction)
                totalRotation+=1;
            System.out.println("possibleDirection: " + possibleDirection.get(i));
            possibleFastestPath.add(aStarSearch(start, goal, mapBit, possibleDirection.get(i)));
            Collections.reverse(possibleFastestPath.get(i));
            System.out.println("Path: for " + i + " is " + possibleFastestPath.get(i));
            callforFun = sendSerialMovement(possibleFastestPath.get(i));
            compareRotation.add(totalRotation);
            totalRotation = 0;
            System.out.println("Rotation: " + compareRotation.get(i));
        }
        for (int i = 0; i < compareRotation.size(); i++) {
            if (lowestNumOfRotate >= compareRotation.get(i)) {
                lowestNumOfRotate = compareRotation.get(i);
                lowestNumOfRotateIndex = i;
//                numOflowest.add(lowestNumOfRotateIndex);
            }
        }
        System.out.println("Result rot: " + compareRotation.get(lowestNumOfRotateIndex));
        for(int i = 0; i < compareRotation.size(); i++){
            if(compareRotation.get(lowestNumOfRotateIndex) == compareRotation.get(i)){
                numOflowest.add(i);
            }
        }
        System.out.println("CCCCCCCpossibleDirection: " + possibleDirection);
        System.out.println("numOflowest: " + numOflowest);
        for(int i = 0; i < numOflowest.size(); i++){
                System.out.println("BBBBBBpossibleDirection.get(i): " + possibleDirection.get(numOflowest.get(i)));
                System.out.println("BBBBBBdirection: " + direction);
            if(possibleDirection.get(numOflowest.get(i)) == direction){
                System.out.println("AAAAAApossibleDirection.get(i): " + possibleDirection.get(numOflowest.get(i)));
                System.out.println("AAAAAAdirection: " + direction);
                
                System.out.println("LOWEST " + compareRotation.get(lowestNumOfRotateIndex));
                Collections.reverse(possibleFastestPath.get(numOflowest.get(i)));
                System.out.println(possibleFastestPath.get(numOflowest.get(i)));
                return possibleFastestPath.get(numOflowest.get(i));
            }            
        }
        System.out.println("AAAAAApossibleDirection.get(i): " + possibleDirection.get(lowestNumOfRotateIndex));
        System.out.println("AAAAAAdirection: " + direction);
                
        System.out.println("I didnt go in if");
        System.out.println("LOWEST " + compareRotation.get(lowestNumOfRotateIndex));
        Collections.reverse(possibleFastestPath.get(lowestNumOfRotateIndex));
        System.out.println(possibleFastestPath.get(lowestNumOfRotateIndex));
        return possibleFastestPath.get(lowestNumOfRotateIndex);
    }

    public ArrayList<Integer> aStarSearch(int start, int goal, int[] mapBit, DIRECTION adirection) {                        //Function of A* search algorithm
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
        DIRECTION starDirection = adirection;
        ArrayList Catch = new ArrayList<Integer>();
        int decisionStartFlag = 0;

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
//                System.out.println("1 FORCE INDEX: " + openSet.get(lowestIndex));
//                decisionStartFlag++;
//            } else {
            lowestIndex = minimumScore(fScore, openSet);
//                decisionStartFlag++;
//            }
            starDirection = directionIndicator(currentIndex, openSet.get(lowestIndex), starDirection);
            currentIndex = openSet.get(lowestIndex);
            openSet.remove(lowestIndex);
            if (currentIndex == goal) {
                midDirection = starDirection;
                //totalRotation = 0;
                return shortestPathResult(cameFrom, currentIndex, start);
            }
            closedSet[currentIndex] = true;
            neighbour = currentNeighbour(currentIndex, starDirection, mapBit);

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
        int start = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);                                                                                     //Start Point
        int end = ConvertToIndex(Constants.GOAL_GRID_ROW, Constants.GOAL_GRID_COL);                                                                                      //Goal Point
        int mid = _mapUI.getMidIndex();                                                                                      //Mid Point
        System.out.println("start shortest path direction: "+direction);
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
        if (_mapUI.getMidIndex() == start || _mapUI.getMidIndex() == end) {
            shortestPath = FastestPath(start, end, mapBit, aDirection);
            Collections.reverse(shortestPath);
        } else {
            shortestPath1 = FastestPath(start, mid, mapBit, aDirection);
            System.out.println("1st: " + direction);
            System.out.println("2nd: " + midDirection);
            shortestPath2 = FastestPath(mid, end, mapBit, midDirection);

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
//        for (int i = 0; i < shortestPath.size(); i++) //Printing of shortest path result
//        {
//            System.out.print(shortestPath.get(i) + " -> ");//@@@@@@@@@@@@@@@@@@@@@@@@ 
//        }
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
                            //System.out.println("grid[" + nextRow + "][" + nextCol + "]");
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

    /* -------------------------------------- Physical Exploration Code ------------------------------------------*/
    public void startPhysicalExplore() {

        System.out.println("\nStarting physical exploration!");

        // Calculate timer intervals based on the user selected steps per second
        _timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
        System.out.println("Steps Per Second: " + _stepsPerSecond + ", Timer Interval: " + _timerIntervals);

        // Calculate number of explored grids required
        _explorationTarget = (int) ((_coverageLimit / 100.0) * ((Constants.MAP_ROWS - 2) * (Constants.MAP_COLS - 2)));
        System.out.println("Exploration target (In grids): " + _explorationTarget);

        // Reset the elapsed exploration time (in milliseconds)
        _elapsedExplorationTime = 0;

        // Reset all variables used
        _phyExploreTimer = null;
        _bPhyExConnected = false;
        _phyExErrors = 0;
        _phyExRcvMsg = null;
        _bPhyExStarted = false;
        _movesSinceLastCalibration = 0;
        testCount = 0;
        _phyExCmdMsg = null;
        _phyExSimMsg = "";

        _phyExploreTimer = new Timer(_timerIntervals, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (!_bPhyExConnected) {
                    mgr = CommsMgr.getCommsMgr();
                    _bPhyExConnected = mgr.setConnection(_timerIntervals - 20);
                    if (_bPhyExConnected) {
                        System.out.println("CONNECTED!!");
                    }

                    if (!_bPhyExConnected) {
                        _phyExErrors++;

                        if (_phyExErrors >= 30) {
                            System.out.println("Too many errors, stopped reconnection!");
                            mgr.closeConnection();

                            if (_phyExploreTimer != null) {
                                _phyExploreTimer.stop();
                                _phyExploreTimer = null;
                            }
                        }
                    }
                    return;
                } else if (_phyExploreTimer != null && _bExplorationComplete) {
//                    _phyExploreTimer.stop();
//                    _phyExploreTimer = null;
                    stopPhysicalExploration();
                    startPhysicalShortestPath(_robotMap.generateShortestPathMap());
                } else if (_bPhyExStarted) {
                    // Make the next move
                    makeNextPhysicalMove();
                    //System.out.println("making next move");
                    // Update elapsed time
                    _elapsedExplorationTime += _timerIntervals;

                } else {
                    // Try to get message
                    _phyExRcvMsg = mgr.recvMsg();
                    //_bPhyExStarted = true;

//                    if (testCount < 1) {
//                        String outputMsg1 = "e";
//                        mgr.sendMsg(outputMsg1, CommsMgr.MSG_TYPE_ARDUINO, false);
//                        testCount++;
//                    }
                    if (_phyExRcvMsg != null && _phyExRcvMsg.equals(START_PHY_EXPLORE)) {
                        _bPhyExStarted = true;
                        String startMsg = "e";
                        mgr.sendMsg(startMsg, CommsMgr.MSG_TYPE_ARDUINO, false);

                        System.out.println("_bPhyExStarted is TRUE!");

                        // Simulate virtual sensors
                        _sensors = simulateSensors(currentRow, currentCol, direction);
                        requestSensorReadings();

                        // Send out first message to Arduino to
                        // do initial calibration and get sensor reading
                        //requestCalibration();
//                        String outputMsg = "AAA;";
//                        CommsMgr.getCommsMgr().sendMsg(outputMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
                    }
                }
            }
        });
        _phyExploreTimer.setRepeats(true);
        _phyExploreTimer.setInitialDelay(0);
        _phyExploreTimer.start();
    }

    /**
     * Function for stopping physical exploration
     */
    public void stopPhysicalExploration() {

        if (_phyExploreTimer != null) {
            _phyExploreTimer.stop();
            _phyExploreTimer = null;
        }

        //System.out.println(CommsMgr.getCommsMgr().recvMsg());
        // Reset all variables
        _phyExploreTimer = null;
        _bPhyExConnected = false;
        _phyExErrors = 0;
        _phyExRcvMsg = null;
        _bPhyExStarted = false;
        _movesSinceLastCalibration = 0;

        System.out.println("Stopping physical exploration!!");
    }

    public void startPhysicalShortestPath(String exploredMap) {
        // Calculate timer intervals based on the user selected steps per second
        _timerIntervals = ((1000 * 1000 / _stepsPerSecond) / 1000);
        System.out.println("Steps Per Second: " + _stepsPerSecond
                + ", Timer Interval: " + _timerIntervals);

        // Reset all variables used
        _phySpTimer = null;
        _bPhySpConnected = true;
        _phySpErrors = 0;
        _phySpRcvMsg = null;
        _bPhySpStarted = false;
        _phySpCmdMsg = null;
        testCount = 0;

        _phySpTimer = new Timer(_timerIntervals, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (!_bPhySpConnected && !_bPhyExConnected) {
                    mgr = CommsMgr.getCommsMgr();

                    //_bPhySpConnected = mgr.isConnected();
                    if (!_bPhySpConnected) {
                        _bPhySpConnected = mgr.setConnection(_timerIntervals - 20);
                    }

                    if (_bPhySpConnected) {
                        System.out.println("startPhysicalSP()" + " -> CONNECTED!!");
                    }

                    if (!_bPhySpConnected) {
                        _phySpErrors++;

                        if (_phySpErrors >= 30) {
                            System.out.println("Too many errors,"
                                    + " stopped reconnection!");
                            mgr.closeConnection();

                            if (_phySpTimer != null) {
                                _phySpTimer.stop();
                                _phySpTimer = null;
                            }
                        }
                    }
                    return;
                }
                
                _phySpRcvMsg = mgr.recvMsg();
                if (!_bPhySpStarted && !_bPhyExStarted) {
                    // Try to get message
                    //_phySpRcvMsg = mgr.recvMsg();
                    
                    if (_phySpRcvMsg == null) {
                        _phySpErrors++;
                        return;
                    }
                    
                    if(_phySpRcvMsg != null){
                        if(_phySpRcvMsg.equals(START_PHY_SP)){
                            System.out.println("Rcv Msg: " + _phySpRcvMsg);
                            System.out.println("_bPhySpStarted is TRUE!");
                            mgr.sendMsg(START_PHY_SP, CommsMgr.MSG_TYPE_ARDUINO, false);
                            _bPhySpStarted = true;
                            _phySpRcvMsg = null;

                        } 
                        else if ((_phySpRcvMsg.length() >= 1 && _phySpRcvMsg.length()<=3 && !_phySpRcvMsg.contains("z"))/*_phySpRcvMsg.contains("m")*/){
                            String midPointMsg = _phySpRcvMsg; //_phySpRcvMsg.substring(1,_phySpRcvMsg.length() - 1);
                            int midIndex = Integer.parseInt(midPointMsg);
                            System.out.println("midIndex: "+midIndex);
                            int midRow = ConvertRow(midIndex) - 1;
                            int midCol = ConvertCol(midIndex) - 1;
                            System.out.println(_mapUI);
                            _mapUI.addMidPoint(midRow, midCol);

                        }    
                    }
                }
                
                if (_phySpRcvMsg!= null && _bPhySpStarted && _phySpRcvMsg.equals(START_PHY_SP)) {

                    String map = exploredMap;
                    int mapBit[] = new int[300];                                                                        //Map in Binary     
                    int start = ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL);                                                                                     //Start Point
                    int end = ConvertToIndex(Constants.GOAL_GRID_ROW, Constants.GOAL_GRID_COL);                                                                                      //Goal Point
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
                    if (_mapUI.getMidIndex() == start || _mapUI.getMidIndex() == end) {
                        shortestPath = FastestPath(start, end, mapBit, aDirection);
                        Collections.reverse(shortestPath);
                    } else {
                        shortestPath1 = FastestPath(start, mid, mapBit, aDirection);
                        shortestPath2 = FastestPath(mid, end, mapBit, midDirection);

                        shortestPath.addAll(shortestPath2);                                                                 //concatenate of 2 Arraylist
                        shortestPath.addAll(shortestPath1);
                        Collections.reverse(shortestPath);
                    }
                        String fPathMsg =  sendSerialMovement(shortestPath); //"WWWWDWWWWWWWWWWWWDWWWWAWWWWWZb";
                        mgr.sendMsg(fPathMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
                        mgr.sendMsg(fPathMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                        simulateShortestPath(shortestPath);
                    
//                    if (_phySpTimer != null) {
//                        _phySpTimer.stop();
//                        _phySpTimer = null;
//                    }
                }
            }
        });
        _phySpTimer.setRepeats(true);
        _phySpTimer.setInitialDelay(0);
        _phySpTimer.start();
    }
    
    public void stopPhysicalShortestPath(){
        if (_phySpTimer != null) {
            _phySpTimer.stop();
            _phySpTimer = null;
        }
        if(mgr != null){
            mgr.closeConnection();
            mgr = null;
        }
    }

    public void makeNextPhysicalMove() {
        // Try to get message
        _phyExRcvMsg = mgr.recvMsg();

        if (_phyExRcvMsg != null || _exploreUnexploredFlag == true) {
            System.out.println("arduino msg: " + _phyExRcvMsg);
            if (_phyExRcvMsg != null && (_phyExRcvMsg.contains("w") || _phyExRcvMsg.contains("a") || _phyExRcvMsg.contains("d") || _phyExRcvMsg.contains("s") || _phyExRcvMsg.contains("c")
                    || _phyExRcvMsg.contains("q") || _phyExRcvMsg.contains("t") || _phyExRcvMsg.contains("x"))) {
                requestSensorReadings();
                validateCount++;
                return;
            } else if ((_phyExRcvMsg == null && _exploreUnexploredFlag == true) || _phyExRcvMsg.length() > 3) {
                if (validateCount >= 1 || _exploreUnexploredFlag == true) {
                    // Sense its surroundings using actual sensor readings
                    if(_phyExRcvMsg != null)
                        this.physicalSense(_phyExRcvMsg);

                    _robotMap.revalidate();
                    _robotMap.repaint();
                    // Logic to make the next move
                    this.physicalPossibleMove();
                    validateCount = 0;
                } else if(startCalibration != 0) {
                    //requestSensorReadings();
                    requestCalibration();
                    startCalibration = 0;
                }
            }
            _phyExRcvMsg = null;
        }
    }

    /**
     * This should update the robot's map based on actual sensor information
     *
     * @param sensorStr The string containing all sensor readings<br>
     * (e.g. "3,5;5;0;0;5;5;")
     */
    private void physicalSense(String sensorStr) {
        //sensorStr = sensorStr.substring(2, sensorStr.length());
        String[] sensorReadings = sensorStr.split(";");
        int sensorIndex = 0;
        // Weightage of the sensors
        double[] sensorWeightage = {1.5, 1.5, 3.0, 3.0, 3.0, 1.0};

        for (Sensor s : _sensors) {

            int obstacleAtGrid = 0;
            try {
                obstacleAtGrid = Integer.parseInt(sensorReadings[sensorIndex]);
                if (obstacleAtGrid < 0) {
                    obstacleAtGrid = s.getMaxRange() + 1;
                }
                System.out.println("sensor index: " + sensorIndex + "free grids: " + (obstacleAtGrid - 1));

                //System.out.println("sensor index: "+ sensorIndex);
            } catch (NumberFormatException e) {
                return;
            }

            int sensorPosRow = s.getSensorPosRow();
            int sensorPosCol = s.getSensorPosCol();
            DIRECTION sensorDir = s.getSensorDirection();
            int sensorMinRange = s.getMinRange();
            int sensorMaxRange = s.getMaxRange();

            /*
			 * System.out.println("Sensor - " + sensorPosRow + ", " +
			 * sensorPosCol + ", " + sensorDir.toString() + ", Free Grids: " +
			 * freeGrids);
             */
            Grid[][] robotMapGrids = _robotMap.getMapGrids();
            for (int currGrid = sensorMinRange; currGrid <= sensorMaxRange; currGrid++) {
                int gridRow = sensorPosRow
                        + ((sensorDir == DIRECTION.NORTH) ? (-1 * currGrid)
                                : (sensorDir == DIRECTION.SOUTH) ? currGrid : 0);
                int gridCol = sensorPosCol
                        + ((sensorDir == DIRECTION.WEST) ? (-1 * currGrid)
                                : (sensorDir == DIRECTION.EAST) ? currGrid : 0);

                double truthValue = 1.0 / (double) currGrid;
                truthValue *= sensorWeightage[sensorIndex];
                // If the current grid is within number of free grids detected
                if (currGrid < obstacleAtGrid) {
                    if ((gridRow >= 0 && gridRow < 20) && (gridCol >= 0 && gridCol < 15)) {
                        robotMapGrids[gridRow][gridCol].markAsFreeGrid(truthValue);
                        robotMapGrids[gridRow][gridCol].setPhantomGrid(false);
//                        robotMapGrids[gridRow][gridCol].setExplored(true);
//                        if ((sensorIndex == 0 || (sensorIndex > 1 && sensorIndex < 5)) && robotMapGrids[gridRow][gridCol].isObstacle()) {
//                            robotMapGrids[gridRow][gridCol].setObstacle(false);
                        if (!robotMapGrids[gridRow][gridCol].isObstacle()) {
                            _phyExSimMsg += ((gridRow * 15) + gridCol) + "f,";
                        }
//                        } 
                    }

                } else if (currGrid > s.getMaxRange()) {
                    break;
                } else if (currGrid == obstacleAtGrid) {

                    // Current grid is less than or equal to max sensor range,
                    // but greater than number of free grids
                    // i.e. current grid is an obstacle
                    //robotMapGrids[gridRow][gridCol].setExplored(true);
                    if (!_robotMap.isStartZone(gridRow, gridCol) && !_robotMap.isGoalZone(gridRow, gridCol)) {
                        if ((gridRow >= 0 && gridRow < 20) && (gridCol >= 0 && gridCol < 15)) {
                            if (!robotMapGrids[gridRow][gridCol].isObstacle()) {
                                robotMapGrids[gridRow][gridCol].markAsObstacle(truthValue);
                                if(robotMapGrids[gridRow][gridCol].getPhantomGrid()){
                                    robotMapGrids[gridRow][gridCol].setPhantomGrid(false);
                                    _phyExSimMsg += ((gridRow * 15) + gridCol) + ",";
                                }
//                                _phyExSimMsg += ((gridRow * 15) + gridCol) + ",";
                            }
                        }
                    }
                    break;
                }
            }
            sensorIndex++;
        }
    }

    public void physicalPossibleMove() {

        if (_movesSinceLastCalibration >= MAX_MOVES_BEFORE_CALIBRATION) {
            if (isLeftWall()) {
                requestCalibration();
                _movesSinceLastCalibration = 0;
                return;
            }
        }
        // Robot reached goal zone
        if (withinGoalZone(currentRow, currentCol)) {
            _bReachedGoal = true;
        }

        if (_bExploreUnexplored) {
            int currentGrid = ConvertToIndex(currentRow, currentCol);
            
            //System.out.println("unreachable: " + _unreachableGrids);
            _unexploredGrids = getUnexploredGrids();
//            System.out.println("unexplored: " + _unexploredGrids);
            if (_unexploredGrids.isEmpty() && currentGrid != ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                moveToStart();
                return;
            } else if (_unexploredGrids.isEmpty() && currentGrid == ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                _bExploreUnexplored = false;
                _exploreUnexploredFlag = true;
                return;
            }
            
            if(_bExploring) {
                exploreUnexplored();
                if (_phyExCmdMsg != null) {
                    System.out.println("explore Unexplored msg: " + _phyExCmdMsg);
                    String outputMsg = _phyExCmdMsg;
                    mgr.sendMsg(outputMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
                    String outputMsg2 = _phyExCmdMsg + "," + _phyExSimMsg;
                    outputMsg2 = outputMsg2.substring(0, outputMsg2.length() - 1);
                    mgr.sendMsg(outputMsg2, CommsMgr.MSG_TYPE_ANDROID, false);
                    _phyExCmdMsg = null;
                    _phyExSimMsg = "";
                }
                return;
            }

            if (!_bExploring) {
                double targetDist = 0;
                double tempDist = 0;
                int reachableGrid = 0;

                for (int i = 0; i < _unexploredGrids.size(); i++) {
                    tempDist = heuristicDist(ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL), _unexploredGrids.get(i));
                    if (tempDist > targetDist) {
                        targetGrid = _unexploredGrids.get(i);
                        targetDist = tempDist;
                    }
                }
                System.out.println("intending to explore unexplored");
                System.out.println("target grid: "+targetGrid);
                reachableGrid = findReachableGrid(targetGrid);
                System.out.println("reachable grid: "+reachableGrid);
                if (reachableGrid != 0) {
                    exploreUnexploredPath = generatePath(currentGrid, reachableGrid); //generate Path
                    System.out.println("explorePath: "+exploreUnexploredPath);
                    if (exploreUnexploredPath == null) {
                        blockAddUnreachable(targetGrid, reachableGrid);
                    } else {
                        if (currentGrid != ConvertToIndex(Constants.START_GRID_ROW, Constants.START_GRID_COL)) {
                            exploreUnexploredPath.add(currentGrid);
                        }
                        Collections.reverse(exploreUnexploredPath);
                        if (exploreUnexploredPath.size() >= 1) {
                            System.out.println("Setting exploring to true.");
                            _bExploring = true;
                            
                            System.out.println("exploring = " +_bExploring);
                        }
                    }
                } else if (!_unreachableGrids.contains(targetGrid)) {
                    _unreachableGrids.add(targetGrid);
                }
            }
            return;
        }

        if (_bReachedGoal && withinStartZone(currentRow, currentCol)) {
            /* with exploreUnexplored */
            _unexploredGrids = getUnexploredGrids();
            if (!_unexploredGrids.isEmpty()) {
                System.out.println("checked all unexplored: " + _bExploreUnexplored);
                System.out.println("unexplored grids: " + _unexploredGrids);
                if(checkTurnLeft(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotateLeft();
                    _phyExCmdMsg = "A";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else if (checkTurnRight(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotateRight();
                    _phyExCmdMsg = "D";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else if (checkTurn180(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotate180();
                    _phyExCmdMsg = "S";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else {
                    requestCalibration();
                    _bExploreUnexplored = true;
                    _exploreUnexploredFlag = true;
                }
//                if (direction != RobotConstant.DEFAULT_START_SP_DIR) {
//                    rotateRight();
//                    _phyExCmdMsg = "D";
//                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
//                    _movesSinceLastCalibration = 0;
//                } 
                
            } else {
                /* without exploreUnexplored */
                if(checkTurnLeft(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotateLeft();
                    _phyExCmdMsg = "A";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else if (checkTurnRight(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotateRight();
                    _phyExCmdMsg = "D";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else if (checkTurn180(direction,RobotConstant.DEFAULT_START_SP_DIR)){
                    rotate180();
                    _phyExCmdMsg = "S";
                    mgr.sendMsg(_phyExCmdMsg, CommsMgr.MSG_TYPE_ANDROID, false);
                    _movesSinceLastCalibration = 0;
                } else{
                    _bExplorationComplete = true;
                    _exploreUnexploredFlag = false;
                    System.out.println("MDF String part 1:" + _robotMap.generateMDFStringPart1());
                    System.out.println("MDF String part 2:" + _robotMap.generateMDFStringPart2());
                    //System.out.println("MDF String:" + _mapUI.generateMapString());
                    _phyExCmdMsg = "Z";
                    String msgToAndroid = "Z," + _robotMap.generateMDFStringPart1() + "," + _robotMap.generateMDFStringPart2();
                    mgr.sendMsg(msgToAndroid, CommsMgr.MSG_TYPE_ANDROID, false);
                }
            }
            if (_phyExCmdMsg != null) {
                String outputMsg = _phyExCmdMsg;
                mgr.sendMsg(outputMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
                _phyExCmdMsg = null;
            }
            return;
        }

        // Exploration complete, do nothing
        if (_bExplorationComplete) {
            return;
        }

        // Robot reached goal zone
        if (withinGoalZone(currentRow, currentCol)) {
            _bReachedGoal = true;
            System.out.println("Reached Goal");
        }

        boolean frontWall = isFrontWall();
        boolean leftWall = isLeftWall();
        boolean rightWall = isRightWall();

        // (No leftWall AND previousLeftWall) OR (frontWall AND No leftWall AND rightWall)
        if ((!leftWall && _bPreviousLeftWall) || (frontWall && !leftWall && rightWall)) {
            rotateLeft();
            _phyExCmdMsg = "A";

        } // (frontWall AND No rightWall)
        else if (frontWall && !rightWall) {
            rotateRight();
            _phyExCmdMsg = "D";
        } // (frontWall AND leftWall AND rightWall)
        else if (frontWall && leftWall && rightWall) {
            rotate180();
            _phyExCmdMsg = "S";
        } else {
            moveForward();
            _phyExCmdMsg = "W";
        }

        // Save current leftWall state into _bPreviousLeftWall
        _bPreviousLeftWall = leftWall;
        _movesSinceLastCalibration++;

        if (_phyExCmdMsg != null) {
            String outputMsg = _phyExCmdMsg;
            //String outputMsgR = "R";
            String outputMsg2 = _phyExCmdMsg + "," + _phyExSimMsg;
            outputMsg2 = outputMsg2.substring(0, outputMsg2.length() - 1);
            mgr.sendMsg(outputMsg, CommsMgr.MSG_TYPE_ARDUINO, false);
            //mgr.sendMsg(outputMsgR, CommsMgr.MSG_TYPE_ARDUINO, false);
            mgr.sendMsg(outputMsg2, CommsMgr.MSG_TYPE_ANDROID, false);
            //requestSensorReadings();
            _phyExSimMsg = "";
            //_phyExCmdMsg = null;
        }
    }

    private void requestSensorReadings() {
        mgr.sendMsg("R", CommsMgr.MSG_TYPE_ARDUINO, false);
    }

    private void requestCalibration() {
        mgr.sendMsg("C", CommsMgr.MSG_TYPE_ARDUINO, false);
    }

}
