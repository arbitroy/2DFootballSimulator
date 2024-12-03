package com.footballsim.models;

import java.io.*;
import java.util.*;

public class GameConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private double fieldWidth;
    private double fieldHeight;
    private List<RobotConfig> robots;
    private List<ObstacleConfig> obstacles;
    private BallConfig ball;
    private int matchDuration; // in seconds
    private double gameSpeed;

    public GameConfig() {
        this.fieldWidth = 600;
        this.fieldHeight = 400;
        this.robots = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.ball = new BallConfig(fieldWidth / 2, fieldHeight / 2);
        this.matchDuration = 300; // 5 minutes default
        this.gameSpeed = 1.0;
    }

    // Nested configuration classes
    public static class RobotConfig implements Serializable {
        private double x, y;
        private boolean isRedTeam;
        private TeamRobot.RobotRole role;
        private double sensorRange;
        private double maxSpeed;

        public RobotConfig(double x, double y, boolean isRedTeam,
                TeamRobot.RobotRole role, double sensorRange, double maxSpeed) {
            this.x = x;
            this.y = y;
            this.isRedTeam = isRedTeam;
            this.role = role;
            this.sensorRange = sensorRange;
            this.maxSpeed = maxSpeed;
        }

        public TeamRobot createRobot() {
            TeamRobot robot = new TeamRobot(x, y, isRedTeam);
            robot.setRole(role);
            return robot;
        }

        // Getters
        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean isRedTeam() {
            return isRedTeam;
        }

        public TeamRobot.RobotRole getRole() {
            return role;
        }

        public double getSensorRange() {
            return sensorRange;
        }

        public double getMaxSpeed() {
            return maxSpeed;
        }
    }

    public static class ObstacleConfig implements Serializable {
        private double x, y;
        private double width, height;
        private Obstacle.ObstacleType type;

        public ObstacleConfig(double x, double y, double width, double height,
                Obstacle.ObstacleType type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
        }

        public Obstacle createObstacle() {
            return new Obstacle(x, y, width, height, type);
        }

        // Getters
        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public Obstacle.ObstacleType getType() {
            return type;
        }
    }

    public static class BallConfig implements Serializable {
        private double x, y;
        private double dx, dy; // Initial velocity

        public BallConfig(double x, double y) {
            this(x, y, 0, 0);
        }

        public BallConfig(double x, double y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        public Ball createBall() {
            Ball ball = new Ball(x, y);
            ball.setVelocity(dx, dy);
            return ball;
        }

        // Getters
        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getDX() {
            return dx;
        }

        public double getDY() {
            return dy;
        }
    }

    // Collection management
    public void addRobot(RobotConfig robot) {
        robots.add(robot);
    }

    public void removeRobot(RobotConfig robot) {
        robots.remove(robot);
    }

    public void addObstacle(ObstacleConfig obstacle) {
        obstacles.add(obstacle);
    }

    public void removeObstacle(ObstacleConfig obstacle) {
        obstacles.remove(obstacle);
    }

    public void clearRobots() {
        robots.clear();
    }

    public void clearObstacles() {
        obstacles.clear();
    }

    // Getters and setters
    public double getFieldWidth() {
        return fieldWidth;
    }

    public void setFieldWidth(double width) {
        this.fieldWidth = width;
    }

    public double getFieldHeight() {
        return fieldHeight;
    }

    public void setFieldHeight(double height) {
        this.fieldHeight = height;
    }

    public List<RobotConfig> getRobots() {
        return Collections.unmodifiableList(robots);
    }

    public List<ObstacleConfig> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    public BallConfig getBall() {
        return ball;
    }

    public void setBall(BallConfig ball) {
        this.ball = ball;
    }

    public int getMatchDuration() {
        return matchDuration;
    }

    public void setMatchDuration(int duration) {
        this.matchDuration = duration;
    }

    public double getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(double speed) {
        this.gameSpeed = speed;
    }

    // File operations
    public void saveToFile(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        }
    }

    public static GameConfig loadFromFile(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (GameConfig) ois.readObject();
        }
    }
}