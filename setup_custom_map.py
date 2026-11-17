import os
import shutil
import nbtlib

src_world = r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\新的世界 (1)'
targets = [
    r'E:\整合\项目\Picking up hope again\Minecraft\versions\1.18.2-Forge_40.3.0\saves\Picking_Up_Hope_World',
    r'E:\整合\项目\Picking up hope again\Minecraft\saves\Picking_Up_Hope_World'
]

for target in targets:
    if os.path.exists(target):
        shutil.rmtree(target)
    os.makedirs(target, exist_ok=True)

    # Copy serverconfig, datapacks, data from source
    for d in ['serverconfig', 'datapacks', 'data']:
        src_d = os.path.join(src_world, d)
        if os.path.exists(src_d):
            shutil.copytree(src_d, os.path.join(target, d), dirs_exist_ok=True)

    # Load source level.dat
    src_level_path = os.path.join(src_world, 'level.dat')
    level = nbtlib.load(src_level_path)
    data = level['Data']

    # 1. Custom Display Name and Game Rules
    data['LevelName'] = nbtlib.String('《重拾希望》星舰灰盒先行版 (Picking Up Hope)')
    data['GameType'] = nbtlib.Int(2)  # Strict Adventure mode (Prevents breaking blocks)
    data['allowCommands'] = nbtlib.Byte(1)  # Cheats allowed
    data['Difficulty'] = nbtlib.Byte(1)

    # 2. Spawn and Space Environment
    data['SpawnX'] = nbtlib.Int(6)
    data['SpawnY'] = nbtlib.Int(62)
    data['SpawnZ'] = nbtlib.Int(10)
    data['SpawnAngle'] = nbtlib.Float(90.0)

    # Midnight space setting
    data['DayTime'] = nbtlib.Long(18000)
    data['Time'] = nbtlib.Long(18000)
    data['raining'] = nbtlib.Byte(0)
    data['thundering'] = nbtlib.Byte(0)

    # Lock weather and day/night cycle
    gamerules = data.get('GameRules', nbtlib.Compound({}))
    gamerules['doDaylightCycle'] = nbtlib.String('false')
    gamerules['doWeatherCycle'] = nbtlib.String('false')
    gamerules['keepInventory'] = nbtlib.String('true')
    gamerules['doMobSpawning'] = nbtlib.String('false')
    data['GameRules'] = gamerules

    # 3. Clean playerdata in level.dat so player spawns at starship center
    if 'Player' in data:
        p = data['Player']
        p['Pos'] = nbtlib.List[nbtlib.Double]([6.5, 62.0, 10.5])
        p['Rotation'] = nbtlib.List[nbtlib.Float]([90.0, 0.0])
        p['Motion'] = nbtlib.List[nbtlib.Double]([0.0, 0.0, 0.0])
        p['OnGround'] = nbtlib.Byte(1)
        p['Air'] = nbtlib.Short(300)
        p['Fire'] = nbtlib.Short(-20)
        p['FallDistance'] = nbtlib.Float(0.0)
        p['HurtTime'] = nbtlib.Short(0)
        # Clear spawn tag so prologue sequence triggers freshly
        if 'Tags' in p:
            tags = [t for t in p['Tags'] if str(t) != 'picking_hope_spawned']
            p['Tags'] = nbtlib.List[nbtlib.String](tags)

    # 4. Pure Space Void World Generator
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
            'features': nbtlib.Byte(0)
        })
    })

    # Save custom level.dat
    dest_level = os.path.join(target, 'level.dat')
    level.save(dest_level, gzipped=True)
    print(f"Custom map generated at: {target}")
