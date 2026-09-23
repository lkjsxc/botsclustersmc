//! CPU-only reward-trained policies and their authoritative curriculum.
pub mod rng;
pub mod network;
pub mod ppo;
pub mod checkpoint;
pub mod reward;
pub mod curriculum;
pub mod next;
pub mod bundle;
pub mod control;

pub const FRAME: usize = 704;
pub const OBS: usize = FRAME * 2 + 12;
/// Movement, angular rates, jump/crouch, interaction, hotbar, GUI operation/slot.
pub const HEADS: [usize; 8] = [9, 7, 5, 3, 6, 10, 6, 90];
pub const ACTIONS: usize = 136;
/// v2 is intentionally incompatible with v1 observations and independent GUI heads.
pub const SCHEMA: u32 = 2;
