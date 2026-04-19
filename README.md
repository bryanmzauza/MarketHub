# MarketHub

Server shop plugin for Paper inspired by RuneScape's Grand Exchange. Prices go up when players buy a lot, and drop when they sell — simple supply and demand.

Works for both Java and Bedrock players out of the box.

## What it does

- Prices move dynamically based on how much players buy/sell (or you can pin them as fixed)
- Java players get a chest GUI, Bedrock players get native Geyser Forms
- Optional NPC trader via Citizens — players click to open the shop
- Supports SQLite by default, MySQL if you need it
- Buy/sell taxes, notifications via actionbar or chat, the usual stuff

## Dependencies

- **Paper 1.21.4+** and **Java 21+**
- **Vault** + some economy plugin (EssentialsX, CMI, etc.)
- *Optional:* **Citizens 2.0.35+** for the trader NPC
- *Optional:* **Floodgate 2.2.3+** for Bedrock forms

## Building from source

```bash
git clone https://github.com/your-user/MarketHub.git
cd MarketHub
./gradlew shadowJar
```

JAR goes to `build/libs/`.

## Setup

1. Drop the JAR in `plugins/`
2. Start the server — config files get generated automatically
3. Edit `config.yml` (database, taxes, etc.) and `shop.yml` (items and prices)
4. `/mh reload` and you're good to go

## Commands

| Command | What it does | Permission |
|---------|--------------|------------|
| `/mh` | Opens the shop | `markethub.use` (default: everyone) |
| `/mh admin add <material> <price>` | Adds an item | `markethub.admin.add` |
| `/mh admin setprice <item> <price>` | Changes base price | `markethub.admin.setprice` |
| `/mh admin remove <item>` | Removes an item | `markethub.admin.remove` |
| `/mh admin reload` | Reloads config | `markethub.admin.reload` |

## Configuring items

Items live in `shop.yml`. Here's what a dynamic item looks like:

```yaml
diamond:
  material: DIAMOND
  display-name: "Diamond"
  category: ores
  price-type: DYNAMIC       # or FIXED
  base-price: 500.0
  virtual-stock: 1000
  target-stock: 1000
  elasticity: 0.3           # how much the price reacts (0.1 = calm, 0.6 = volatile)
  price-min: 100.0
  price-max: 2000.0
```

When `virtual-stock` drops below `target-stock`, price goes up. When it rises above, price goes down. `elasticity` controls how aggressive the swings are.

Items set to `FIXED` just ignore all the stock/elasticity stuff and keep the base price forever.

## Config overview

Everything is in `config.yml` with comments explaining each option. The highlights:

- **Database** — `sqlite` (zero setup) or `mysql`
- **Taxes** — separate rates for buying from and selling to the server
- **Dynamic pricing** — how often prices update, decay factor
- **NPC** — enable/disable, custom name and skin

## License

[MIT](LICENSE)
