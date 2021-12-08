package Sharustry.world.blocks.defense;

import Sharustry.content.SFx;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;

public class ExplodeMine extends Block {
    public float cooldown = 50f;
    public int shots = 6;
    public float shotsSpacing = 0f;
    public @Nullable BulletType bullet;
    public float inaccuracy = 0f;
    public float teamAlpha = 0.3f;
    public TextureRegion teamRegion;

    public ExplodeMine(String name) {
        super(name);
        update = false;
        destructible = true;
        solid = false;
        targetable = false;
        hasShadow = false;
    }

    @Override
    public void load() {
        super.load();
        teamRegion = Core.atlas.find(name + "-team-top");
    }

    public class ExplodeMineBuild extends Building {
        boolean once = false;

        @Override
        public void drawTeam(){
            //no
        }

        @Override
        public void drawCracks(){
            //no
        }

        @Override
        public void draw(){
            super.draw();
            if(!Core.atlas.isFound(teamRegion)) return;
            Draw.color(team.color, teamAlpha);
            Draw.rect(teamRegion, x, y);
            Draw.color();
        }

        @Override
        public void unitOn(Unit unit){
            if(enabled && unit.team != team && !once){
                triggered();
                once = true;
            }
        }

        public void triggered(){
            SFx.mineExplode.at(x, y, 0, cooldown);
            if(bullet != null){
                Time.run(cooldown, () -> {
                    if(dead) return;
                    for(int i = 0; i < shots; i++)  {
                        int finalI = i;
                        Time.run(i * shotsSpacing/60, () -> {
                            Units.nearbyEnemies(team, x, y, 30 * 8f, u -> {
                                bullet.create(this, u.x, u.y, (360f / shots) * finalI + Mathf.random(inaccuracy));
                            });
                        });
                    }
                    kill();
                });
            }
        }
    }
}
