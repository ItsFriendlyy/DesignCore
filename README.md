# DesignCore

**DesignCore** is an all-in-one chat styling, nametag hierarchy, and player social management plugin engineered natively for modern **Paper** and **Purpur** servers (Minecraft 1.20.5 – 1.21.x) using the **Adventure Component API** and **Java 21**.

It replaces bloated chat and prefix management suites with an asynchronous, zero-flicker architecture featuring scoreboard team sorting, LuckPerms rank inheritance, a persistent friendship system, and native hook compatibility for staff moderation plugins like **StaffCore**.

---

## ✦ Features

- **Modern Async Chat Engine:** Fully asynchronous chat formatting leveraging Adventure components. Supports legacy formatting (`&c`), Hex/RGB colors, and permission checks (`designcore.colorchat`).
- **Dynamic Overhead Nametags & Tablist:** Automatic scoreboard team priority ordering (`dc_0001_...`). Ranks order in descending priority in the tablist and player nametags without flickering or display bugs.
- **LuckPerms Weight Inheritance & Custom Overrides:** Automatically maps LuckPerms primary groups and priorities, while providing live in-game priority adjustments and per-player prefix/suffix overrides.
- **Persistent Social Friend Network:**
  - Robust UUID-to-Name caching to eliminate broken `unknown` player names across reboots and offline sessions.
  - Complete request lifecycle (`/friend add`, `/friend accept`, `/friend deny`, `/friend remove`, `/friend list`).
  - Active friendship tenure calculation (e.g., `3d 12h`).
  - Direct message privacy GUI (`/pmsettings`) allowing players to restrict DMs to **Everyone**, **Friends Only**, or **No One**.
- **Private Messaging & Surveillance:**
  - Fast `/msg` and `/reply` pipeline with offline player caching and sound triggers.
  - Staff Spy monitoring (`/spym`) allowing authorized personnel to monitor server-wide direct messages in real time.
- **Vanish & Connection Interoperability:**
  - Formatted `[+]` and `[-]` join and quit broadcasts with rank colors, prefixes, and suffixes.
  - Native hook integration with moderation suites (such as **StaffCore**) to silence join/quit broadcasts on silent connections and issue authentic fake join/leave broadcasts upon toggling vanish.
- **PlaceholderAPI Hook:** Provides custom placeholders for scoreboards, menus, and external displays:
  - `%designcore_prefix%`
  - `%designcore_suffix%`
  - `%designcore_group%`
  - `%designcore_color%`

---

## ✦ Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/design` | Main administrative configuration for groups, priorities, and prefixes | `designcore.admin` | `op` |
| `/msg <player> <message>` | Send a private direct message to a player | `designcore.msg` | `true` |
| `/reply <message>` | Reply to the last player who messaged you | `designcore.msg` | `true` |
| `/friend <add\|accept\|deny\|remove\|list>` | Manage friend requests and view friend status | *None* | `true` |
| `/unfriend <player>` | Remove a player from your friends list | *None* | `true` |
| `/pmsettings` | Open the interactive Direct Message privacy settings GUI | *None* | `true` |
| `/mutechat` | Toggle server-wide public chat on/off | `designcore.mutechat` | `op` |
| `/spym` | Toggle private message surveillance mode for staff | `designcore.spym` | `op` |

---

## ✦ Installation & Setup

1. Verify that your server environment runs **Java 21** and **Paper / Purpur 1.20.5+**.
2. *(Optional)* Install **LuckPerms** and **PlaceholderAPI** for rank synchronization and placeholder support.
3. Download the latest release from the [Releases](../../releases) tab or build from source.
4. Place `DesignCore.jar` into your server's `plugins/` folder.
5. Restart your server.
