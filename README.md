# SoulSMP

Players drop a glowing **Lost Soul** when they die. Souls are the key ingredient for forging 6 legendary weapons, each with a right-click ability and a passive.

## Build
Requires Java 21 + Maven.
    mvn package
Jar ends up in target/SoulSMP.jar -> drop into your Paper 1.21.11 plugins folder.

## Resource pack
Upload SoulSMP-ResourcePack.zip somewhere with a direct link, then in server.properties set:
    resource-pack=<direct link>
    resource-pack-sha1=<sha1 of the zip>   (sha1sum SoulSMP-ResourcePack.zip)
    require-resource-pack=true
Pack is built for 1.21.11 (pack format 75, min_format/max_format in pack.mcmeta).

## Weapons (all forged in a crafting table, S = Lost Soul)
| Weapon | Base | Recipe | Ability |
|---|---|---|---|
| Voidreaver Scythe | Netherite Hoe | ESE / SHS / _B_  (E ender eye, H netherite hoe, B blaze rod) | Void Rift: pull + blind in 8 blocks. Passive lifesteal |
| Stormcaller Blade | Diamond Sword | _LS / LDL / SL_  (L lightning rod, D diamond sword) | Tempest Call: lightning + chain x3. Passive shock |
| Emberfang Axe | Netherite Axe | MSM / SAS / MSM  (M magma block, A netherite axe) | Inferno Nova: fire ring + knockback. Passive ignite |
| Frostbite Dagger | Iron Sword | BSB / _I_ / _S_  (B blue ice, I iron sword) | Glacial Dash: freezing dash. Passive chill |
| Wraith Scepter | Blaze Rod | NSN / SRS / NSN  (N netherite scrap, R blaze rod) | Haunting Bolt: homing wither bolt |
| Gravity Hammer | Mace | OSO / SMS / OSO  (O obsidian, M mace) | Gravity Slam: launch then slam. Passive launch |

## Name + lore lock
The texture is attached via a custom_model_data string, and the plugin only treats an item as genuine if the hidden tag, base material, exact name and exact lore all match.
- Genuine weapons/souls cannot be renamed or combined in an anvil.
- If name/lore ever stop matching, the model is stripped and abilities stop working.
- Renaming a normal item to "Voidreaver Scythe" gives no texture and no abilities.

## Commands (soulsmp.admin, default op)
/soulsmp list
/soulsmp give <player> <weapon|soul> [amount]

## Easiest way to get the jar (no installs)
1. Make a free GitHub repo and upload this whole folder (keep the .github folder).
2. Open the Actions tab -> "Build SoulSMP" -> wait ~1 minute.
3. Download the "SoulSMP-jar" artifact -> put SoulSMP.jar in your server's plugins folder.
