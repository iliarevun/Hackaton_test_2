package com.example.shop.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "userBody")
@Data
public class UserBody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // ── Basic body params ──
    private int age;
    private int height;
    private float weight;
    private String gender;

    // ── Goal & activity ──
    private String goal;           // LOSE_WEIGHT / GAIN_MUSCLE / STAY_FIT / ENDURANCE / FLEXIBILITY
    private String activityLevel;  // Beginner / Medium / Advanced

    // ── Equipment (comma-separated list of selected items) ──
    @Column(name = "equipment", length = 500)
    private String equipment;      // e.g. "Dumbbells,Resistance Bands,Pull-up Bar"

    // ── Workout preferences ──
    @Column(name = "workout_location")
    private String workoutLocation; // HOME / GYM / OUTDOOR

    @Column(name = "workout_days_per_week")
    private int workoutDaysPerWeek; // 2-6

    @Column(name = "workout_duration_minutes")
    private int workoutDurationMinutes; // 20/30/45/60/90

    // ── Health info ──
    @Column(name = "health_conditions", length = 500)
    private String healthConditions; // comma-separated or free text

    @Column(name = "injuries", length = 500)
    private String injuries;         // comma-separated or free text

    @Column(name = "health_notes", length = 1000)
    private String healthNotes;      // free text

    // ── Goal label helpers ──
    public String getGoalLabel() {
        if (goal == null) return "Not specified";
        return switch (goal) {
            case "LOSE_WEIGHT"  -> "Lose Weight";
            case "GAIN_MUSCLE"  -> "Gain Muscle";
            case "STAY_FIT"     -> "Stay Fit";
            case "ENDURANCE"    -> "Endurance";
            case "FLEXIBILITY"  -> "Flexibility";
            default -> goal;
        };
    }

    public String getGoalEmoji() {
        if (goal == null) return "🦸";
        return switch (goal) {
            case "LOSE_WEIGHT"  -> "🔥";
            case "GAIN_MUSCLE"  -> "💪";
            case "STAY_FIT"     -> "⚡";
            case "ENDURANCE"    -> "🏃";
            case "FLEXIBILITY"  -> "🧘";
            default -> "🦸";
        };
    }
}
