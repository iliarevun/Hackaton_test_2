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
    private String equipment;      // e.g. "Гантелі,Гумові стрічки,Турнік"

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
        if (goal == null) return "Не вказано";
        return switch (goal) {
            case "LOSE_WEIGHT"  -> "Схуднути";
            case "GAIN_MUSCLE"  -> "Набрати м'язи";
            case "STAY_FIT"     -> "Тримати форму";
            case "ENDURANCE"    -> "Витривалість";
            case "FLEXIBILITY"  -> "Гнучкість";
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
