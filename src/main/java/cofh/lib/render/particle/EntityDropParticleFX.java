package cofh.lib.render.particle;

import cofh.lib.util.helpers.MathHelper;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
//import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Look like EntityFX now just handled by Particle class. Extending that instead.
//TODO: This probably does weird stuff as I've just added in the speed params without altering anything else.
//Test & clean up.

@SideOnly(Side.CLIENT)
public class EntityDropParticleFX extends Particle {

	private int bobTimer;

	public EntityDropParticleFX(World world, double x, double y, double z, double xSpeedIn, double ySpeedIn, double zSpeedIn, float particleRed, float particleGreen, float particleBlue) {
		//constuctor changed, need speeds now too & set particle colors
		this(world, x, y, z, xSpeedIn, ySpeedIn, zSpeedIn, particleRed, particleGreen, particleBlue,-1);
	}

	public EntityDropParticleFX(World world, double x, double y, double z, double xSpeedIn, double ySpeedIn, double zSpeedIn, float particleRed, float particleGreen, float particleBlue, int gravityMod) {

		super(world, x, y, z, xSpeedIn, ySpeedIn, zSpeedIn);
		this.motionX = this.motionY = this.motionZ = 0.0D;

		this.particleRed = particleRed;
		this.particleGreen = particleGreen;
		this.particleBlue = particleBlue;

		this.setParticleTextureIndex(113);
		this.setSize(0.01F, 0.01F);
		this.particleGravity = -0.06F * gravityMod;
		this.bobTimer = 40;
		this.particleMaxAge = (int) (48.0D / (Math.random() * 0.8D + 0.2D));
		this.motionX = this.motionY = this.motionZ = 0.0D;
	}

	@Override
	public void onUpdate() {

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		this.motionY -= this.particleGravity;

		if (this.bobTimer-- > 0) {
			this.motionX *= 0.02D;
			this.motionY *= 0.02D;
			this.motionZ *= 0.02D;
			this.setParticleTextureIndex(113);
		} else {
			this.setParticleTextureIndex(112);
		}
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= 0.9800000190734863D;
		this.motionY *= 0.9800000190734863D;
		this.motionZ *= 0.9800000190734863D;

		if (this.particleMaxAge-- <= 0) {
			this.setExpired();
		}
		//no ground check, just checking collision instead
		if (this.isCollided) {
			this.setParticleTextureIndex(114);
			this.motionX *= 0.699999988079071D;
			this.motionZ *= 0.699999988079071D;
		}
		BlockPos pos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY), MathHelper.floor(this.posZ));
		IBlockState state = this.worldObj.getBlockState(pos);

		if (this.particleGravity > 0) {
			Material material = state.getBlock().getMaterial(state);
			if (material.isLiquid() || material.isSolid()) {
				double d0 = MathHelper.floor(this.posY) + 1 - BlockLiquid.getLiquidHeightPercent(state.getBlock().getMetaFromState(state));
				if (this.posY < d0) {
					this.setExpired();
				}
			}
		} else {
			Material material = state.getBlock().getMaterial(state);

			if (material.isLiquid() || material.isSolid()) {
				double d0 = MathHelper.ceil(this.posY) + 1 - BlockLiquid.getLiquidHeightPercent(state.getBlock().getMetaFromState(state));
				if (this.posY > d0) {
					this.setExpired();
				}
			}
		}
	}
}
