package Sharustry.world.blocks.defense.turret.mounts;

import Sharustry.world.blocks.defense.turret.MultiTurret;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import mindustry.content.Fx;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Fires;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.ItemImage;
import mindustry.ui.MultiReqImage;
import mindustry.ui.ReqImage;
import mindustry.world.Tile;

import static arc.struct.ObjectMap.of;
import static mindustry.Vars.*;

public class LiquidMountTurretType extends MountTurretType {
    public ObjectMap<Liquid, BulletType> ammoTypes;
    public boolean extinguish = false;

    public LiquidMountTurretType(String name, BulletType bullet, Object... ammos) {
        super(name, bullet, ammos);
        ammoTypes = OrderedMap.of(ammos);
        shootSound = Sounds.none;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
    }
    @Override
    public MountTurret create(MultiTurret block, MultiTurret.MultiTurretBuild build, int index, float x, float y) {
        return new LiquidMountTurret(this, block, build, index, x, y);
    }
    @Override
    public ObjectMap<ObjectMap<BulletType, ? extends UnlockableContent>, TextureRegion> getStatData() {
        ObjectMap<ObjectMap<BulletType, ? extends UnlockableContent>, TextureRegion> types = new ObjectMap<>();
        for(Liquid liquid : content.liquids()) {
            BulletType bullet = ammoTypes.get(liquid);
            if(bullet != null) types.put(of(bullet, liquid), liquid.uiIcon);
        }
        return types;
    }
    public class LiquidMountTurret extends MountTurret<LiquidMountTurretType> {
        public LiquidMountTurret(LiquidMountTurretType type, MultiTurret block, MultiTurret.MultiTurretBuild build, int i, float x, float y) {
            super(type, block, build, i, x, y);
        }

        @Override
        public boolean acceptLiquid(Liquid liquid){
            return type.ammoTypes != null && type.ammoTypes.get(liquid) != null && (build.liquids.current() == liquid || (type.ammoTypes.containsKey(liquid)
                    && (!type.ammoTypes.containsKey(build.liquids.current()) || build.liquids.get(build.liquids.current()) <= 1f / type.ammoTypes.get(build.liquids.current()).ammoMultiplier + 0.001f)));
        }

        @Override
        public void findTarget() {
            if(type.extinguish && build.liquids.current().canExtinguish()) {
                int tr = (int) (type.range / tilesize);
                for(int x = -tr; x <= tr; x++) for(int y = -tr; y <= tr; y++) {
                    Tile other = world.tileWorld(x + x / 8f, y + y / 8f);
                    //do not extinguish fires on other team blocks
                    if (other != null && Fires.has(x + x / 8, y + y /8) && (other.build == null || other.team() == build.team))
                        target = Fires.get(x + x / 8, y + y / 8);
                }
            } else {
                super.findTarget();
            }
        }

        @Override
        public BulletType peekAmmo() {
            return type.ammoTypes.get(build.liquids.current());
        }

        @Override
        public BulletType useAmmo() {
            return build.cheating() ? peekAmmo() : super.useAmmo();
        }

        @Override
        public boolean hasAmmo() {
            return  build.liquids != null
                    && type.ammoTypes != null
                    && type.ammoTypes.get(build.liquids.current()) != null
                    && build.liquids.currentAmount() >= 1f / type.ammoTypes.get(build.liquids.current()).ammoMultiplier;
        }

        @Override
        public void display(Table table){
            if(block.basicMounts.size > 3 && mountIndex % 4 == 0) table.row();
            else if(mountIndex % 4 == 0) table.row();
            table.stack(
                    new Table(o -> {
                        o.left();
                        o.image(Core.atlas.find("shar-" + type.name + "-full")).size(5*8f);
                    }),
                    new Table(h -> {
                        h.stack(
                            new Table(e -> {
                                e.defaults().growX().height(9).width(42f).padRight(2*8).padTop(8*2f);
                                e.add(hasAmmo() && block.hasLiquids
                                        ? new Bar("", build.liquids.current().color, () -> build.liquids.get(build.liquids.current()) / block.liquidCapacity)
                                        : new Bar("", new Color(0.1f, 0.1f, 0.1f, 1), () -> 0));
                                e.pack();
                            }),
                            new Table(e -> {
                                e.defaults().growX().height(9).width(42f).padRight(2*8).padTop(8*5f);
                                e.add(new Bar(() -> "", () -> Pal.accent.cpy().lerp(Color.orange, reloadCounter / type.reload), () -> reloadCounter / type.reload));
                                e.pack();
                            }),
                            new Table(e -> {
                                if(type.chargeTime <= 0) return;
                                e.defaults().growX().height(9).width(42f).padRight(2*8).padTop(8*8f);
                                e.add(new Bar(() -> "", () -> Pal.surge.cpy().lerp(Pal.accent, reloadCounter / type.reload), () -> charge));
                                e.pack();
                            }),
                            hasAmmo()
                                ? new Table(e -> e.add(new ItemImage(build.liquids.current().fullIcon, totalAmmo)))
                                : new Table(e -> {
                                    MultiReqImage liquidReq = new MultiReqImage();
                                    for(Liquid liquid : type.ammoTypes.keys()) liquidReq.add(new ReqImage(liquid.uiIcon, () -> hasAmmo()));
                                    e.add(liquidReq).size(18f);
                                })
                        ).padTop(2*8).padLeft(2*8);
                        h.pack();
                    })
            ).left().size(7*8f);
        }
    }
}