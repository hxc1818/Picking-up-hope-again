import os
import shutil
import nbtlib

src_world = r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\新的世界 (1)'
primary_targets = [
    r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\Picking_Up_Hope_World',
    r'E:\整合\项目\Picking up hope again\Minecraft\saves\Picking_Up_Hope_World',
]
secondary_saves = [
    r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\新的世界',
    r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\新的世界 (1)',
]

# Read source level.dat and template dirs into memory first before touching disk
src_level_path = os.path.join(src_world, 'level.dat')
template_level_bytes = open(src_level_path, 'rb').read()
template_dirs = {}
for d in ['serverconfig', 'datapacks', 'data']:
    src_d = os.path.join(src_world, d)
    if os.path.exists(src_d):
        tmp_copy = os.path.join(os.path.dirname(src_world), f'__tmp_{d}')
        if os.path.exists(tmp_copy):
            shutil.rmtree(tmp_copy)
        shutil.copytree(src_d, tmp_copy)
        template_dirs[d] = tmp_copy


def sanitize_level(level_obj, display_name):
    data = level_obj['Data']
    data['LevelName'] = nbtlib.String(display_name)
    data['GameType'] = nbtlib.Int(2)  # Adventure mode
    data['allowCommands'] = nbtlib.Byte(1)
    data['Difficulty'] = nbtlib.Byte(1)

    data['SpawnX'] = nbtlib.Int(6)
    data['SpawnY'] = nbtlib.Int(61)
    data['SpawnZ'] = nbtlib.Int(10)
    data['SpawnAngle'] = nbtlib.Float(270.0)

    data['DayTime'] = nbtlib.Long(18000)
    data['Time'] = nbtlib.Long(18000)
    data['raining'] = nbtlib.Byte(0)
    data['thundering'] = nbtlib.Byte(0)

    gamerules = data.get('GameRules', nbtlib.Compound({}))
    gamerules['doDaylightCycle'] = nbtlib.String('false')
    gamerules['doWeatherCycle'] = nbtlib.String('false')
    gamerules['keepInventory'] = nbtlib.String('true')
    gamerules['doMobSpawning'] = nbtlib.String('false')
    data['GameRules'] = gamerules

    if 'Player' in data:
        p = data['Player']
        p['playerGameType'] = nbtlib.Int(2)
        p['previousPlayerGameType'] = nbtlib.Int(2)
        p['Pos'] = nbtlib.List[nbtlib.Double]([6.5, 61.0, 10.5])
        p['Rotation'] = nbtlib.List[nbtlib.Float]([270.0, 0.0])
        p['Motion'] = nbtlib.List[nbtlib.Double]([0.0, 0.0, 0.0])
        p['OnGround'] = nbtlib.Byte(1)
        p['Air'] = nbtlib.Short(300)
        p['Fire'] = nbtlib.Short(-20)
        p['FallDistance'] = nbtlib.Float(0.0)
        p['HurtTime'] = nbtlib.Short(0)
        p['Health'] = nbtlib.Float(20.0)
        p['XpLevel'] = nbtlib.Int(0)
        p['XpP'] = nbtlib.Float(0.0)
        p['XpTotal'] = nbtlib.Int(0)
        p['Inventory'] = nbtlib.List[nbtlib.Compound]([])
        for stale_key in ('ForgeCaps', 'ForgeData', 'Tags', 'EnderItems'):
            if stale_key in p:
                del p[stale_key]
        p['abilities'] = nbtlib.Compound({
            'flying': nbtlib.Byte(0),
            'flySpeed': nbtlib.Float(0.05),
            'instabuild': nbtlib.Byte(0),
            'invulnerable': nbtlib.Byte(0),
            'mayBuild': nbtlib.Byte(0),
            'mayfly': nbtlib.Byte(0),
            'walkSpeed': nbtlib.Float(0.1),
        })

    wgs = data.get('WorldGenSettings', nbtlib.Compound({}))
    dims = wgs.get('dimensions', nbtlib.Compound({}))
    overworld = dims.get('minecraft:overworld', nbtlib.Compound({}))
    overworld['generator'] = nbtlib.Compound({
        'type': nbtlib.String('minecraft:flat'),
        'settings': nbtlib.Compound({
            'biome': nbtlib.String('minecraft:the_void'),
            'layers': nbtlib.List[nbtlib.Compound]([
                nbtlib.Compound({'block': nbtlib.String('minecraft:air'), 'height': nbtlib.Int(1)})
            ]),
            'structures': nbtlib.Compound({'structures': nbtlib.Compound({})}),
            'features': nbtlib.Byte(0),
        })
    })
    dims['minecraft:overworld'] = overworld
    wgs['dimensions'] = dims
    data['WorldGenSettings'] = wgs


def rebuild_save_dir(target_dir, display_name):
    if os.path.exists(target_dir):
        shutil.rmtree(target_dir)
    os.makedirs(target_dir, exist_ok=True)
    for d, tmp_copy in template_dirs.items():
        shutil.copytree(tmp_copy, os.path.join(target_dir, d), dirs_exist_ok=True)
    dest_level = os.path.join(target_dir, 'level.dat')
    with open(dest_level, 'wb') as f:
        f.write(template_level_bytes)
    lvl = nbtlib.load(dest_level)
    sanitize_level(lvl, display_name)
    lvl.save(dest_level, gzipped=True)
    print(f"Reset clean v2.0 save: {target_dir} -> '{display_name}'")


for t in primary_targets:
    rebuild_save_dir(t, '《重拾希望》v2.0 回合制剧情版 (Picking Up Hope)')

for idx, t in enumerate(secondary_saves):
    rebuild_save_dir(t, f'《重拾希望》v2.0 备用重置存档 #{idx + 1}')

for tmp_copy in template_dirs.values():
    if os.path.exists(tmp_copy):
        shutil.rmtree(tmp_copy)
