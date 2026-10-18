<p align="center">
  <img src="Logo.png" alt="AdvancedAdminTools" width="160">
</p>

<h1 align="center">🛡️ AdvancedAdminTools</h1>

<p align="center">
  <b>All the admin tools your Minecraft server needs, in one plugin and one menu.</b><br>
  Spigot / Paper 1.18+ · Java 17
</p>

<p align="center">
  🇬🇧 <a href="#-english">English</a> · 🇫🇷 <a href="#-français">Français</a>
</p>

---

## 🇬🇧 English

### ✨ Highlights

- 🧰 **One GUI for everything** – `/tools` lists online players, click one to moderate, inspect or help them.
- 🎒 **Live invsee** – see *and edit* a player's inventory, armor and offhand in real time.
- 👻 **Real vanish** – fully invisible (armor and held items included), with toggleable options so you never get caught.
- 📜 **Sanction history** – every ban, mute, warn and kick is saved.
- 🔒 **Server control** – maintenance mode, chat lock with whitelist, announcements, command spy.

### 🔨 Moderation

| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> <time\|permanent> <reason>` | 🚫 Ban a player | `aat.ban` |
| `/unban <player>` | ✅ Unban a player | `aat.unban` |
| `/mute <player> <time\|permanent> <reason>` | 🔇 Mute a player | `aat.mute` |
| `/unmute <player>` | 🔊 Unmute a player | `aat.unmute` |
| `/warn <player> <reason>` | ⚠️ Warn a player | `aat.warn` |
| `/kick <player> [reason]` | 👢 Kick a player | `aat.kick` |
| `/history <player>` | 📜 Sanction history | `aat.history` |
| `/sc <message>` | 💬 Staff-only chat | `aat.staffchat` |

⏱️ Time format: `30s`, `10m`, `2h`, `3d` or `permanent`.

### 🔍 Inspection

| Command | Description | Permission |
|---------|-------------|------------|
| `/tools` | 🧰 Open the admin menu | `aat.tools` |
| `/invsee <player>` | 🎒 View & edit inventory live (armor + offhand included) | `aat.invsee` |
| `/endersee <player>` | 🟪 View & edit ender chest live | `aat.endersee` |
| `/clearinv <player>` | 🗑️ Clear inventory (confirmation menu) | `aat.clearinv` |
| `/pinfo <player>` | ℹ️ IP, ping, location, health, status… | `aat.pinfo` |
| `/alts <player>` | 👥 Accounts sharing the same IP | `aat.alts` |
| `/tpto <player>` · `/back` | 🌀 Teleport to a player, then go back | `aat.tp` |

### 👻 Vanish

| Command | Description | Permission |
|---------|-------------|------------|
| `/vanish [on\|off]` | Toggle vanish (kept after reconnecting, silent join/quit) | `aat.vanish` |
| `/vanish options` | ⚙️ Options menu | `aat.vanish` |

Options (all **off** by default, click to toggle): 🧲 pick up items · 🫸 push / be pushed · 🧟 mobs target you · 🪨 trigger pressure plates · 🗡️ take damage · 👀 see other vanished staff (**on** by default) · 🤫 silent containers (**on** by default).

🤫 **Silent containers**: chests, trapped chests, barrels, shulker boxes and ender chests open with no animation, no sound, no redstone signal and no sculk trigger. You can still take and put items, live.

### 🛠️ Utilities

| Command | Description | Permission |
|---------|-------------|------------|
| `/heal [player]` | ❤️ Heal | `aat.heal` |
| `/feed [player]` | 🍗 Feed | `aat.feed` |
| `/gm <0\|1\|2\|3> [player]` | 🎮 Change gamemode | `aat.gamemode` |
| `/fly [player]` | 🕊️ Toggle fly | `aat.fly` |
| `/god [player]` | 🛡️ Toggle god mode | `aat.god` |

### 🌍 Server

| Command | Description | Permission |
|---------|-------------|------------|
| `/maintenance` | 🚧 Toggle maintenance (non-staff are kicked) | `aat.maintenance` |
| `/maintenance add\|remove <player>` · `list` | Maintenance whitelist | `aat.maintenance` |
| `/chatlock` | 🔒 Lock / unlock the chat | `aat.chatlock` |
| `/chatlock add\|remove <player>` · `list` | Players allowed to talk while locked | `aat.chatlock` |
| `/clearchat` | 🧹 Clear the chat | `aat.clearchat` |
| `/bc <message>` | 📢 Announcement (title + chat, `&` colors) | `aat.broadcast` |
| `/cmdspy` | 🕵️ See commands typed by players | `aat.cmdspy` |

### 🔑 Extra permissions

| Permission | Description |
|------------|-------------|
| `aat.*` | ⭐ Everything |
| `aat.notify` | 🔔 Alerts (banned player joining, muted player chatting) |
| `aat.chatlock.bypass` | Talk while the chat is locked |
| `aat.maintenance.bypass` | Join during maintenance |

All permissions are given to OPs by default.

---

## 🇫🇷 Français

### ✨ Points forts

- 🧰 **Un seul menu pour tout** – `/tools` liste les joueurs connectés, clique sur l'un d'eux pour le modérer, l'inspecter ou l'aider.
- 🎒 **Invsee en direct** – voir *et modifier* l'inventaire, l'armure et la main secondaire d'un joueur en temps réel.
- 👻 **Vrai vanish** – totalement invisible (armure et objets en main compris), avec des options activables pour ne jamais se faire griller.
- 📜 **Historique des sanctions** – chaque ban, mute, warn et kick est enregistré.
- 🔒 **Contrôle du serveur** – maintenance, chat verrouillé avec whitelist, annonces, espion de commandes.

### 🔨 Modération

| Commande | Description | Permission |
|----------|-------------|------------|
| `/ban <joueur> <durée\|permanent> <raison>` | 🚫 Bannir un joueur | `aat.ban` |
| `/unban <joueur>` | ✅ Débannir un joueur | `aat.unban` |
| `/mute <joueur> <durée\|permanent> <raison>` | 🔇 Rendre muet un joueur | `aat.mute` |
| `/unmute <joueur>` | 🔊 Rendre la parole | `aat.unmute` |
| `/warn <joueur> <raison>` | ⚠️ Avertir un joueur | `aat.warn` |
| `/kick <joueur> [raison]` | 👢 Expulser un joueur | `aat.kick` |
| `/history <joueur>` | 📜 Casier du joueur | `aat.history` |
| `/sc <message>` | 💬 Chat réservé au staff | `aat.staffchat` |

⏱️ Format de durée : `30s`, `10m`, `2h`, `3d` ou `permanent`.

### 🔍 Inspection

| Commande | Description | Permission |
|----------|-------------|------------|
| `/tools` | 🧰 Ouvrir le menu admin | `aat.tools` |
| `/invsee <joueur>` | 🎒 Voir et modifier l'inventaire en direct (armure + main secondaire) | `aat.invsee` |
| `/endersee <joueur>` | 🟪 Voir et modifier l'enderchest en direct | `aat.endersee` |
| `/clearinv <joueur>` | 🗑️ Vider l'inventaire (menu de confirmation) | `aat.clearinv` |
| `/pinfo <joueur>` | ℹ️ IP, ping, position, vie, statuts… | `aat.pinfo` |
| `/alts <joueur>` | 👥 Comptes avec la même IP | `aat.alts` |
| `/tpto <joueur>` · `/back` | 🌀 Se téléporter à un joueur, puis revenir | `aat.tp` |

### 👻 Vanish

| Commande | Description | Permission |
|----------|-------------|------------|
| `/vanish [on\|off]` | Activer / désactiver (gardé après reconnexion, connexion/déconnexion silencieuse) | `aat.vanish` |
| `/vanish options` | ⚙️ Menu des options | `aat.vanish` |

Options (toutes **désactivées** par défaut, clic pour changer) : 🧲 ramasser les objets · 🫸 pousser / être poussé · 🧟 être ciblé par les monstres · 🪨 déclencher les plaques de pression · 🗡️ prendre des dégâts · 👀 voir les autres staff en vanish (**activée** par défaut) · 🤫 coffres silencieux (**activée** par défaut).

🤫 **Coffres silencieux** : coffres, coffres piégés, tonneaux, shulkers et enderchests s'ouvrent sans animation, sans son, sans signal redstone et sans déclencher les sculk sensors. Tu peux quand même prendre et poser des objets, en direct.

### 🛠️ Utilitaires

| Commande | Description | Permission |
|----------|-------------|------------|
| `/heal [joueur]` | ❤️ Soigner | `aat.heal` |
| `/feed [joueur]` | 🍗 Nourrir | `aat.feed` |
| `/gm <0\|1\|2\|3> [joueur]` | 🎮 Changer de mode de jeu | `aat.gamemode` |
| `/fly [joueur]` | 🕊️ Activer / désactiver le vol | `aat.fly` |
| `/god [joueur]` | 🛡️ Activer / désactiver l'invincibilité | `aat.god` |

### 🌍 Serveur

| Commande | Description | Permission |
|----------|-------------|------------|
| `/maintenance` | 🚧 Activer / désactiver la maintenance (les non-staff sont expulsés) | `aat.maintenance` |
| `/maintenance add\|remove <joueur>` · `list` | Whitelist de maintenance | `aat.maintenance` |
| `/chatlock` | 🔒 Verrouiller / déverrouiller le chat | `aat.chatlock` |
| `/chatlock add\|remove <joueur>` · `list` | Joueurs autorisés à parler quand le chat est verrouillé | `aat.chatlock` |
| `/clearchat` | 🧹 Vider le chat | `aat.clearchat` |
| `/bc <message>` | 📢 Annonce (titre + chat, couleurs avec `&`) | `aat.broadcast` |
| `/cmdspy` | 🕵️ Voir les commandes tapées par les joueurs | `aat.cmdspy` |

### 🔑 Permissions supplémentaires

| Permission | Description |
|------------|-------------|
| `aat.*` | ⭐ Tout |
| `aat.notify` | 🔔 Alertes (joueur banni qui tente de se connecter, joueur muet qui parle) |
| `aat.chatlock.bypass` | Parler quand le chat est verrouillé |
| `aat.maintenance.bypass` | Se connecter pendant la maintenance |

Toutes les permissions sont données aux OP par défaut.

---

<p align="center">Made with ❤️ by <b>Caraito</b> · MIT License</p>
