package com.footballsim.formations;

import com.footballsim.models.*;
import java.util.*;

/**
 * Manages team formations, positioning, and role assignments
 */
public class FormationManager {
    private final FieldManager fieldManager;
    private Formation currentFormation;
    private boolean isRedTeam;
    private Map<TeamRobot, FormationPosition> robotPositions;

    /**
     * Represents a formation position with role and relative coordinates
     */
    public static class FormationPosition {
        private final TeamRobot.RobotRole role;
        private final double relativeX; // 0-1 relative to field width
        private final double relativeY; // 0-1 relative to field height

        public FormationPosition(TeamRobot.RobotRole role, double relativeX, double relativeY) {
            this.role = role;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }

        public TeamRobot.RobotRole getRole() {
            return role;
        }

        public double getRelativeX() {
            return relativeX;
        }

        public double getRelativeY() {
            return relativeY;
        }
    }

    /**
     * Represents a team formation with defined positions
     */
    public static class Formation {
        private final String name;
        private final List<FormationPosition> positions;
        private final FormationType type;

        public enum FormationType {
            ATTACKING,
            DEFENSIVE,
            BALANCED
        }

        public Formation(String name, FormationType type, List<FormationPosition> positions) {
            this.name = name;
            this.type = type;
            this.positions = positions;
        }

        public String getName() {
            return name;
        }

        public FormationType getType() {
            return type;
        }

        public List<FormationPosition> getPositions() {
            return Collections.unmodifiableList(positions);
        }
    }

    /**
     * Creates predefined formations
     */
    public static class FormationFactory {
        public static Formation create2_2() {
            List<FormationPosition> positions = Arrays.asList(
                    new FormationPosition(TeamRobot.RobotRole.GOALKEEPER, 0.1, 0.5),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.3, 0.3),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.3, 0.7),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.7, 0.3),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.7, 0.7));
            return new Formation("2-2", Formation.FormationType.BALANCED, positions);
        }

        public static Formation create1_2_1() {
            List<FormationPosition> positions = Arrays.asList(
                    new FormationPosition(TeamRobot.RobotRole.GOALKEEPER, 0.1, 0.5),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.3, 0.5),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.6, 0.3),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.6, 0.7),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.8, 0.5));
            return new Formation("1-2-1", Formation.FormationType.ATTACKING, positions);
        }

        public static Formation create2_1_1() {
            List<FormationPosition> positions = Arrays.asList(
                    new FormationPosition(TeamRobot.RobotRole.GOALKEEPER, 0.1, 0.5),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.3, 0.3),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.3, 0.7),
                    new FormationPosition(TeamRobot.RobotRole.DEFENDER, 0.5, 0.5),
                    new FormationPosition(TeamRobot.RobotRole.ATTACKER, 0.8, 0.5));
            return new Formation("2-1-1", Formation.FormationType.DEFENSIVE, positions);
        }
    }

    /**
     * Creates a formation manager for a team
     * 
     * @param fieldManager The field manager
     * @param isRedTeam    Whether this is for the red team
     */
    public FormationManager(FieldManager fieldManager, boolean isRedTeam) {
        this.fieldManager = fieldManager;
        this.isRedTeam = isRedTeam;
        this.robotPositions = new HashMap<>();
        this.currentFormation = FormationFactory.create2_2(); // Default formation
    }

    public void clearFormations() {
        robotPositions.clear();
    }

    /**
     * Assigns roles and positions to robots based on current formation
     * 
     * @param robots List of team robots
     */
    public void assignPositions(List<TeamRobot> robots) {
        robotPositions.clear();  // Clear old mappings first
        List<FormationPosition> positions = currentFormation.getPositions();

        // Mirror positions for blue team
        if (!isRedTeam) {
            positions = mirrorPositions(positions);
        }

        // Assign positions to available robots
        Iterator<TeamRobot> robotIterator = robots.iterator();
        Iterator<FormationPosition> positionIterator = positions.iterator();

        while (robotIterator.hasNext() && positionIterator.hasNext()) {
            TeamRobot robot = robotIterator.next();
            FormationPosition position = positionIterator.next();
            robot.setRole(position.getRole());
            robotPositions.put(robot, position);
        }
    }

    public void removeRobot(TeamRobot robot) {
        robotPositions.remove(robot);
    }

    /**
     * Mirrors formation positions for the opposing team
     * 
     * @param positions Original positions
     * @return Mirrored positions
     */
    private List<FormationPosition> mirrorPositions(List<FormationPosition> positions) {
        List<FormationPosition> mirrored = new ArrayList<>();
        for (FormationPosition pos : positions) {
            mirrored.add(new FormationPosition(
                    pos.getRole(),
                    1.0 - pos.getRelativeX(),
                    pos.getRelativeY()));
        }
        return mirrored;
    }

    /**
     * Gets target position for a robot
     * 
     * @param robot The robot
     * @param ball  The ball
     * @return Target position as double[]{x, y}
     */
    public double[] getTargetPosition(TeamRobot robot, Ball ball) {
        FormationPosition basePosition = robotPositions.get(robot);
        if (basePosition == null)
            return null;

        // Convert relative positions to actual field coordinates
        double baseX = fieldManager.getBoundaries()[0].getXY()[0] +
                basePosition.getRelativeX() * fieldManager.getBoundaries()[2].getXY()[2];
        double baseY = fieldManager.getBoundaries()[0].getXY()[1] +
                basePosition.getRelativeY() * fieldManager.getBoundaries()[2].getXY()[3];

        // Adjust position based on ball position and role
        return adjustPositionForRole(robot.getCurrentRole(), baseX, baseY, ball);
    }

    /**
     * Adjusts position based on robot's role and ball position
     * 
     * @param role  Robot's role
     * @param baseX Base X position
     * @param baseY Base Y position
     * @param ball  The ball
     * @return Adjusted position
     */
    private double[] adjustPositionForRole(TeamRobot.RobotRole role, double baseX, double baseY, Ball ball) {
        double[] adjusted = new double[] { baseX, baseY };

        switch (role) {
            case GOALKEEPER:
                // Stay on goal line, move up/down based on ball
                adjusted[1] = Math.max(Math.min(ball.getY(),
                        fieldManager.getBoundaries()[2].getXY()[1] - 50),
                        fieldManager.getBoundaries()[0].getXY()[1] + 50);
                break;

            case DEFENDER:
                // Move towards ball if it's in defensive half
                if (isInDefensiveHalf(ball.getX())) {
                    adjusted[0] += (ball.getX() - adjusted[0]) * 0.3;
                    adjusted[1] += (ball.getY() - adjusted[1]) * 0.3;
                }
                break;

            case ATTACKER:
                // Move more freely towards ball in attacking positions
                adjusted[0] += (ball.getX() - adjusted[0]) * 0.5;
                adjusted[1] += (ball.getY() - adjusted[1]) * 0.5;
                break;
        }

        return adjusted;
    }

    /**
     * Checks if position is in defensive half
     * 
     * @param x X coordinate to check
     * @return true if in defensive half
     */
    private boolean isInDefensiveHalf(double x) {
        double midField = (fieldManager.getBoundaries()[2].getXY()[2] +
                fieldManager.getBoundaries()[0].getXY()[0]) / 2;
        return (isRedTeam && x < midField) || (!isRedTeam && x > midField);
    }

    /**
     * Changes the current formation
     * 
     * @param newFormation The formation to change to
     */
    public void changeFormation(Formation newFormation) {
        this.currentFormation = newFormation;
    }

    /**
     * Gets the current formation
     * 
     * @return Current formation
     */
    public Formation getCurrentFormation() {
        return currentFormation;
    }
}