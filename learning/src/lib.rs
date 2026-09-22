//! Dependency-free, CPU-only on-policy learning. No demonstrations or task scripts.
pub mod rng;
pub mod network;
pub mod ppo;
pub mod checkpoint;
pub mod reward;

pub const FRAME: usize = 640;
pub const OBS: usize = FRAME * 2 + 12;
/// Movement, yaw, pitch, jump/crouch, interaction, hotbar, GUI operation, GUI slot.
pub const HEADS: [usize; 8] = [9, 7, 5, 3, 6, 10, 6, 90];
pub const ACTIONS: usize = 136;
pub const SCHEMA: u32 = 1;

pub mod curriculum;
