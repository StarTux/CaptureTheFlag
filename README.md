# CaptureTheFlag

CaptureTheFlag plugin.

## Commands

### `/ctf`

**Permission** `ctf.admin`

Alias of `/capturetheflag`

### `/ctfadm`

**Permission** `ctf.ctf`

Alias of `/capturetheflagadmin`

## Areas

The area file is named `CaptureTheFlag`.

- `game` Mark the game area(s) where chunks should be loaded
- `merchant` Named to mark where merchants should spawn:
  - `armor`
  - `supply`
  - `weapon`
- `creep` Where monsters can spawn

For example:
- `/area add CaptureTheFlag game`
- `/area add CaptureTheFlag merchant armor`

### Team Areas

Named after each team: `red` and `blue`

- `spawn` to mark spawn areas of each team
- `flag` to mark the flag spawn of each team

For example:
- `/area add CaptureTheFlag spawn red`
- `/area add CaptureTheFlag flag blue`