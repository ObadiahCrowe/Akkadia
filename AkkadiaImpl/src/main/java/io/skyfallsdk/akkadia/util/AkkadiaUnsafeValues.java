package io.skyfallsdk.akkadia.util;

import com.esotericsoftware.asm.*;
import io.skyfallsdk.Server;
import io.skyfallsdk.akkadia.Akkadia;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.UnsafeValues;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.*;

public class AkkadiaUnsafeValues implements UnsafeValues {

    private static final Set<String> EVIL = new HashSet<>( Arrays.asList(
      "org/bukkit/World (III)I getBlockTypeIdAt",
      "org/bukkit/World (Lorg/bukkit/Location;)I getBlockTypeIdAt",
      "org/bukkit/block/Block ()I getTypeId",
      "org/bukkit/block/Block (I)Z setTypeId",
      "org/bukkit/block/Block (IZ)Z setTypeId",
      "org/bukkit/block/Block (IBZ)Z setTypeIdAndData",
      "org/bukkit/block/Block (B)V setData",
      "org/bukkit/block/Block (BZ)V setData",
      "org/bukkit/inventory/ItemStack ()I getTypeId",
      "org/bukkit/inventory/ItemStack (I)V setTypeId"
    ) );

    private static final Map<String, String> SEARCH_AND_REMOVE = initReplacementsMap();
    private static Map<String, String> initReplacementsMap()
    {
        Map<String, String> getAndRemove = new HashMap<>();
        // Be wary of maven shade's relocations
        getAndRemove.put( "org/bukkit/".concat( "craftbukkit/libs/it/unimi/dsi/fastutil/" ), "org/bukkit/".concat( "craftbukkit/libs/" ) ); // Remap fastutil to our location

        if ( Boolean.getBoolean( "debug.rewriteForIde" ) )
        {
            // unversion incoming calls for pre-relocate debug work
            final String NMS_REVISION_PACKAGE = "v1_16_R3/";

            getAndRemove.put( "net/minecraft/".concat( "server/" + NMS_REVISION_PACKAGE ), NMS_REVISION_PACKAGE );
            getAndRemove.put( "org/bukkit/".concat( "craftbukkit/" + NMS_REVISION_PACKAGE ), NMS_REVISION_PACKAGE );
        }

        return getAndRemove;
    }

    @Override
    public Material toLegacy(Material material) {
        return null;
    }

    @Override
    public Material fromLegacy(Material material) {
        return null;
    }

    @Override
    public Material fromLegacy(MaterialData material) {
        return null;
    }

    @Override
    public Material fromLegacy(MaterialData material, boolean itemPriority) {
        return null;
    }

    @Override
    public BlockData fromLegacy(Material material, byte data) {
        return null;
    }

    @Override
    public Material getMaterial(String material, int version) {
        return null;
    }

    @Override
    public int getDataVersion() {
        return 0;
    }

    @Override
    public ItemStack modifyItemStack(ItemStack stack, String arguments) {
        return null;
    }

    @Override
    public void checkSupported(PluginDescriptionFile pdf) throws InvalidPluginException {

    }

    @Override
    public byte[] processClass(PluginDescriptionFile pdf, String path, byte[] clazz) {
        try {
            clazz = this.convert(clazz, !isLegacy(pdf));
        } catch (Exception ex) {
            Server.get().getExpansion(Akkadia.class).getLogger().fatal("Fatal error trying to convert " + pdf.getFullName() + ":" + path, ex);
        }

        return clazz;
    }

    private boolean isLegacy(PluginDescriptionFile pdf) {
        return pdf.getAPIVersion() == null;
    }

    private byte[] convert(byte[] clazz, boolean modern) {
        ClassReader cr = new ClassReader( clazz );
        ClassWriter cw = new ClassWriter( cr, 0 );

        cr.accept( new ClassVisitor( Opcodes.ASM5, cw )
        {
            // Paper start - Rewrite plugins
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
            {
                desc = getOriginalOrRewrite( desc );
                if ( signature != null ) {
                    signature = getOriginalOrRewrite( signature );
                }

                return super.visitField( access, name, desc, signature, value) ;
            }
            // Paper end

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
                return new MethodVisitor( api, super.visitMethod( access, name, desc, signature, exceptions ) )
                {
                    // Paper start - Plugin rewrites
                    @Override
                    public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments)
                    {
                        // Paper start - Rewrite plugins
                        name = getOriginalOrRewrite( name );
                        if ( desc != null )
                        {
                            desc = getOriginalOrRewrite( desc );
                        }
                        // Paper end

                        super.visitInvokeDynamicInsn( name, desc, bootstrapMethodHandle, bootstrapMethodArguments );
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type)
                    {
                        type = getOriginalOrRewrite( type );

                        super.visitTypeInsn( opcode, type );
                    }

                    @Override
                    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                        for ( int i = 0; i < local.length; i++ )
                        {
                            if ( !( local[i] instanceof String ) ) { continue; }

                            local[i] = getOriginalOrRewrite( (String) local[i] );
                        }

                        for ( int i = 0; i < stack.length; i++ )
                        {
                            if ( !( stack[i] instanceof String ) ) { continue; }

                            stack[i] = getOriginalOrRewrite( (String) stack[i] );
                        }

                        super.visitFrame( type, nLocal, local, nStack, stack );
                    }

                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index)
                    {
                        descriptor = getOriginalOrRewrite( descriptor );

                        super.visitLocalVariable( name, descriptor, signature, start, end, index );
                    }
                    // Paper end

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc)
                    {
                        // Paper start - Rewrite plugins
                        owner = getOriginalOrRewrite( owner );
                        if ( desc != null )
                        {
                            desc = getOriginalOrRewrite( desc );
                        }
                        // Paper end

                        if ( owner.equals( "org/bukkit/block/Biome" ) )
                        {
                            switch ( name )
                            {
                                case "NETHER":
                                    super.visitFieldInsn( opcode, owner, "NETHER_WASTES", desc );
                                    return;
                            }
                        }

                        if ( owner.equals( "org/bukkit/entity/EntityType" ) )
                        {
                            switch ( name )
                            {
                                case "PIG_ZOMBIE":
                                    super.visitFieldInsn( opcode, owner, "ZOMBIFIED_PIGLIN", desc );
                                    return;
                            }
                        }

                        if ( modern )
                        {
                            if ( owner.equals( "org/bukkit/Material" ) )
                            {
                                switch ( name )
                                {
                                    case "CACTUS_GREEN":
                                        name = "GREEN_DYE";
                                        break;
                                    case "DANDELION_YELLOW":
                                        name = "YELLOW_DYE";
                                        break;
                                    case "ROSE_RED":
                                        name = "RED_DYE";
                                        break;
                                    case "SIGN":
                                        name = "OAK_SIGN";
                                        break;
                                    case "WALL_SIGN":
                                        name = "OAK_WALL_SIGN";
                                        break;
                                    case "ZOMBIE_PIGMAN_SPAWN_EGG":
                                        name = "ZOMBIFIED_PIGLIN_SPAWN_EGG";
                                        break;
                                }
                            }

                            super.visitFieldInsn( opcode, owner, name, desc );
                            return;
                        }

                        if ( owner.equals( "org/bukkit/Material" ) )
                        {
                            try
                            {
                                Material.valueOf( "LEGACY_" + name );
                            } catch ( IllegalArgumentException ex )
                            {
                                throw new AuthorNagException( "No legacy enum constant for " + name + ". Did you forget to define a modern (1.13+) api-version in your plugin.yml?" );
                            }

                            super.visitFieldInsn( opcode, owner, "LEGACY_" + name, desc );
                            return;
                        }

                        if ( owner.equals( "org/bukkit/Art" ) )
                        {
                            switch ( name )
                            {
                                case "BURNINGSKULL":
                                    super.visitFieldInsn( opcode, owner, "BURNING_SKULL", desc );
                                    return;
                                case "DONKEYKONG":
                                    super.visitFieldInsn( opcode, owner, "DONKEY_KONG", desc );
                                    return;
                            }
                        }

                        if ( owner.equals( "org/bukkit/DyeColor" ) )
                        {
                            switch ( name )
                            {
                                case "SILVER":
                                    super.visitFieldInsn( opcode, owner, "LIGHT_GRAY", desc );
                                    return;
                            }
                        }

                        if ( owner.equals( "org/bukkit/Particle" ) )
                        {
                            switch ( name )
                            {
                                case "BLOCK_CRACK":
                                case "BLOCK_DUST":
                                case "FALLING_DUST":
                                    super.visitFieldInsn( opcode, owner, "LEGACY_" + name, desc );
                                    return;
                            }
                        }

                        super.visitFieldInsn( opcode, owner, name, desc );
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
                    {
                        // SPIGOT-4496
                        if ( owner.equals( "org/bukkit/map/MapView" ) && name.equals( "getId" ) && desc.equals( "()S" ) )
                        {
                            // Should be same size on stack so just call other method
                            super.visitMethodInsn( opcode, owner, name, "()I", itf );
                            return;
                        }
                        // SPIGOT-4608
                        if ( (owner.equals( "org/bukkit/Bukkit" ) || owner.equals( "org/bukkit/Server" ) ) && name.equals( "getMap" ) && desc.equals( "(S)Lorg/bukkit/map/MapView;" ) )
                        {
                            // Should be same size on stack so just call other method
                            super.visitMethodInsn( opcode, owner, name, "(I)Lorg/bukkit/map/MapView;", itf );
                            return;
                        }

                        // Paper start - Rewrite plugins
                        owner = getOriginalOrRewrite( owner) ;
                        if (desc != null)
                        {
                            desc = getOriginalOrRewrite(desc);
                        }
                        // Paper end

                        if ( modern )
                        {
                            if ( owner.equals( "org/bukkit/Material" ) )
                            {
                                switch ( name )
                                {
                                    case "values":
                                        super.visitMethodInsn( opcode, "org/bukkit/craftbukkit/util/CraftLegacy", "modern_" + name, desc, itf );
                                        return;
                                    case "ordinal":
                                        super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/bukkit/craftbukkit/util/CraftLegacy", "modern_" + name, "(Lorg/bukkit/Material;)I", false );
                                        return;
                                }
                            }

                            super.visitMethodInsn( opcode, owner, name, desc, itf );
                            return;
                        }

                        if ( owner.equals( "org/bukkit/ChunkSnapshot" ) && name.equals( "getBlockData" ) && desc.equals( "(III)I" ) )
                        {
                            super.visitMethodInsn( opcode, owner, "getData", desc, itf );
                            return;
                        }

                        Type retType = Type.getReturnType( desc );

                        if ( EVIL.contains( owner + " " + desc + " " + name )
                          || ( owner.startsWith( "org/bukkit/block/" ) && ( desc + " " + name ).equals( "()I getTypeId" ) )
                          || ( owner.startsWith( "org/bukkit/block/" ) && ( desc + " " + name ).equals( "(I)Z setTypeId" ) )
                          || ( owner.startsWith( "org/bukkit/block/" ) && ( desc + " " + name ).equals( "()Lorg/bukkit/Material; getType" ) ) )
                        {
                            Type[] args = Type.getArgumentTypes( desc );
                            Type[] newArgs = new Type[ args.length + 1 ];
                            newArgs[0] = Type.getObjectType( owner );
                            System.arraycopy( args, 0, newArgs, 1, args.length );

                            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/bukkit/craftbukkit/legacy/CraftEvil", name, Type.getMethodDescriptor( retType, newArgs ), false );
                            return;
                        }

                        if ( owner.equals( "org/bukkit/DyeColor" ) )
                        {
                            if ( name.equals( "valueOf" ) && desc.equals( "(Ljava/lang/String;)Lorg/bukkit/DyeColor;" ) )
                            {
                                super.visitMethodInsn( opcode, owner, "legacyValueOf", desc, itf );
                                return;
                            }
                        }

                        if ( owner.equals( "org/bukkit/Material" ) )
                        {
                            if ( name.equals( "getMaterial" ) && desc.equals( "(I)Lorg/bukkit/Material;" ) )
                            {
                                super.visitMethodInsn( opcode, "org/bukkit/craftbukkit/legacy/CraftEvil", name, desc, itf );
                                return;
                            }

                            switch ( name )
                            {
                                case "values":
                                case "valueOf":
                                case "getMaterial":
                                case "matchMaterial":
                                    super.visitMethodInsn( opcode, "org/bukkit/craftbukkit/legacy/CraftLegacy", name, desc, itf );
                                    return;
                                case "ordinal":
                                    super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/bukkit/craftbukkit/legacy/CraftLegacy", "ordinal", "(Lorg/bukkit/Material;)I", false );
                                    return;
                                case "name":
                                case "toString":
                                    super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/bukkit/craftbukkit/legacy/CraftLegacy", name, "(Lorg/bukkit/Material;)Ljava/lang/String;", false );
                                    return;
                            }
                        }

                        if ( retType.getSort() == Type.OBJECT && retType.getInternalName().equals( "org/bukkit/Material" ) && owner.startsWith( "org/bukkit" ) )
                        {
                            super.visitMethodInsn( opcode, owner, name, desc, itf );
                            super.visitMethodInsn( Opcodes.INVOKESTATIC, "org/bukkit/craftbukkit/legacy/CraftLegacy", "toLegacy", "(Lorg/bukkit/Material;)Lorg/bukkit/Material;", false );
                            return;
                        }

                        super.visitMethodInsn( opcode, owner, name, desc, itf );
                    }
                };
            }
        }, 0 );

        return cw.toByteArray();
    }

    private String getOriginalOrRewrite(String original) {
        String rewrite = null;
        for ( Map.Entry<String, String> entry : SEARCH_AND_REMOVE.entrySet() )
        {
            if ( original.contains( entry.getKey() ) )
            {
                rewrite = original.replace( entry.getValue(), "" );
            }
        }

        return rewrite != null ? rewrite : original;
    }

    @Override
    public Advancement loadAdvancement(NamespacedKey key, String advancement) {
        return null;
    }

    @Override
    public boolean removeAdvancement(NamespacedKey key) {
        return false;
    }
}
