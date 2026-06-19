# PrismDuels

Custom duel plugin for Prism SMP. Paper 1.21.1.

## Features
- **Regular Duels** (`/duel <player>`) - Fight with your real inventory. Winner keeps items.
- **Practice Duels** (`/practiceduel <player> <gamemode>`) - Preset MCTiers kits. Inventory fully restored after.
- **9 Gamemodes**: Sword, Crystal, Mace, SpearMace, Axe, DiaPot, UHC, SMP, DiaSMP
- **Stats Tracking**: Wins, losses, KD, streaks (tracked separately for regular and practice)
- **Stats GUI**: `/duelstats [player]` opens a chest GUI with all stats
- **PlaceholderAPI**: `%prismduel_wins%`, `%prismduel_kd%`, `%prismduel_streak%`, etc.
- **Admin Controls**: Enable/disable duels, manage arenas, reload config

## Setup

1. Download the jar from GitHub Actions artifacts
2. Drop in `/plugins/`
3. Start the server
4. Build your duel arena(s) anywhere on the server
5. Set arena spawn points:
   - Stand at spawn 1: `/duels setarena <name>`
   - Stand at spawn 2: `/duels setspawn2`
6. Repeat for additional arenas

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/duel <player>` | `prismduel.use` | Challenge to regular duel |
| `/duel accept/deny` | `prismduel.use` | Accept or deny a request |
| `/practiceduel <player> <mode>` | `prismduel.use` | Challenge to practice duel |
| `/duelstats [player]` | `prismduel.use` | View duel statistics GUI |
| `/duels enable/disable` | `prismduel.admin` | Toggle duel system |
| `/duels setarena <name>` | `prismduel.admin` | Set arena spawn 1 |
| `/duels setspawn2` | `prismduel.admin` | Set arena spawn 2 |
| `/duels removearena <name>` | `prismduel.admin` | Remove an arena |
| `/duels listarenas` | `prismduel.admin` | List all arenas |
| `/duels reload` | `prismduel.admin` | Reload config/kits |

## Placeholders

| Placeholder | Description |
|------------|-------------|
| `%prismduel_wins%` | Total wins |
| `%prismduel_losses%` | Total losses |
| `%prismduel_kd%` | KD ratio |
| `%prismduel_winrate%` | Win rate % |
| `%prismduel_streak%` | Best win streak |
| `%prismduel_regular_wins%` | Regular duel wins |
| `%prismduel_practice_wins%` | Practice duel wins |

## Kit Configuration

Default MCTiers kits are generated on first run in `plugins/PrismDuels/kits.yml`. Edit the file and run `/duels reload` to apply changes.

## Dependencies

- **Required**: Paper 1.21.1
- **Optional**: PlaceholderAPI (for stat placeholders)
