package robot;

import java.awt.Color;

public final class RobotConstant {
	
	// Robot size
	public static final int ROBOT_SIZE = 3;
	
	// G values used for A* algorithm
	public static final int MOVE_COST = 1;
	public static final int TURN_COST = 20;
	
	// Sensors default range (In grids)
	public static final int SHORT_IR_MIN = 1;
	public static final int SHORT_IR_MAX = 4;
	
	public static final int LONG_IR_MIN = 2;
	public static final int LONG_IR_MAX = 7;
	
	public static enum DIRECTION {
		NORTH, EAST, SOUTH, WEST;

		public static DIRECTION getNext(DIRECTION currDirection) {
			return values()[(currDirection.ordinal() + 1) % values().length];
		}
		
		public static DIRECTION getPrevious(DIRECTION currDirection) {
			return values()[(currDirection.ordinal() + values().length - 1)
					% values().length];
		}
		
		/**
		 * Use at your own discretion
		 * 
		 * @param direction The direction to be converted into an enum
		 * @return Enum representing specified direction
		 */
		public static DIRECTION fromString(String direction) {
			return valueOf(direction.toUpperCase());
		}
	};
	
	// Colors for rendering the map
	public static final Color C_BORDER = Color.BLACK;
	
	public static final Color C_LINE = Color.ORANGE;
	public static final int LINE_WEIGHT = 2;
	
	public static final Color C_START = Color.BLUE;
	public static final Color C_GOAL = Color.GREEN;
	
	public static final Color C_UNEXPLORED = Color.LIGHT_GRAY;
	public static final Color C_FREE = Color.WHITE;
	public static final Color C_OBSTACLE = Color.BLACK;
	
	// For rendering the robot in the robot map
	public static final Color C_ROBOT_OUTLINE = new Color(0, 0, 0, 220);
	public static final Color C_ROBOT = new Color(0, 205, 255, 160);
	public static final Color C_ROBOT_FRONT = new Color(0, 46, 155, 220);
	
	// For rendering the robot path in the robot map
	public static final Color C_EXPLORE_PATH = Color.RED;
	public static final Color C_SHORTEST_PATH = Color.ORANGE;
	public static final int PATH_THICKNESS = 4;
	
	public static final Color C_SENSOR = Color.DARK_GRAY;
	public static final Color C_SENSOR_BEAM_OUTER = new Color(220, 0, 0, 160);
	public static final Color C_SENSOR_BEAM_INNER = new Color(255, 0, 0, 190);
	
	// Robot Default Configuration
	public static final int DEFAULT_START_ROW = 17; // Changed to 1 based on ROBOT_SIZE
	public static final int DEFAULT_START_COL = 0;
	public static final DIRECTION DEFAULT_START_DIR = DIRECTION.EAST;
	
	// Robot Exploration Configuration
	public static final int DEFAULT_STEPS_PER_SECOND = 10;
	public static final int DEFAULT_COVERAGE_LIMIT = 50;
	public static final int DEFAULT_TIME_LIMIT = 360;
	
	
	// Prevent instantiation
	private RobotConstant() {}
}
