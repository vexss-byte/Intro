package dev.introcinema.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All network messages. Server -> client: Start, Sync, Reveal, End. Client -> server: ClientStatus.
 * A silhouette member's real name is NOT in Start; it only arrives in Reveal.
 */
public sealed interface IntroPayload extends CustomPacketPayload
        permits IntroPayload.Start, IntroPayload.Sync, IntroPayload.Reveal, IntroPayload.End, IntroPayload.ClientStatus {

    static <T extends CustomPacketPayload> Type<T> payloadType(String path) {
        return new Type<>(Identifier.fromNamespaceAndPath("intro_member", path));
    }

    record Member(UUID uuid, int number, String name, boolean silhouette, String quote, boolean last) {
        void write(RegistryFriendlyByteBuf b) {
            b.writeUUID(uuid); b.writeVarInt(number); b.writeUtf(name); b.writeBoolean(silhouette); b.writeUtf(quote); b.writeBoolean(last);
        }
        static Member read(RegistryFriendlyByteBuf b) {
            return new Member(b.readUUID(), b.readVarInt(), b.readUtf(), b.readBoolean(), b.readUtf(), b.readBoolean());
        }
    }

    record Start(String sessionId, int startTick, String title, String subtitle, String lastLabel,
                 int opening, int memberTicks, int finale, int returnTicks, String quality,
                 boolean cameraOn, boolean animationOn, boolean shaderOn, boolean bloomOn, boolean screenFxOn,
                 float cameraSpeed, List<Member> members) implements IntroPayload {
        public static final Type<Start> TYPE = IntroPayload.payloadType("start");
        public static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC = StreamCodec.ofMember(Start::write, Start::read);
        @Override public Type<Start> type() { return TYPE; }

        void write(RegistryFriendlyByteBuf b) {
            b.writeUtf(sessionId); b.writeVarInt(startTick); b.writeUtf(title); b.writeUtf(subtitle); b.writeUtf(lastLabel);
            b.writeVarInt(opening); b.writeVarInt(memberTicks); b.writeVarInt(finale); b.writeVarInt(returnTicks); b.writeUtf(quality);
            b.writeBoolean(cameraOn); b.writeBoolean(animationOn); b.writeBoolean(shaderOn); b.writeBoolean(bloomOn); b.writeBoolean(screenFxOn);
            b.writeFloat(cameraSpeed);
            b.writeVarInt(members.size());
            for (Member m : members) m.write(b);
        }
        static Start read(RegistryFriendlyByteBuf b) {
            String id = b.readUtf(); int st = b.readVarInt(); String t = b.readUtf(), s = b.readUtf(), l = b.readUtf();
            int o = b.readVarInt(), m = b.readVarInt(), f = b.readVarInt(), r = b.readVarInt(); String q = b.readUtf();
            boolean c = b.readBoolean(), a = b.readBoolean(), sh = b.readBoolean(), bl = b.readBoolean(), sf = b.readBoolean();
            float sp = b.readFloat();
            int n = b.readVarInt();
            List<Member> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(Member.read(b));
            return new Start(id, st, t, s, l, o, m, f, r, q, c, a, sh, bl, sf, sp, list);
        }
    }

    /** Server clock heartbeat. Late, duplicate or out-of-order packets are harmless: the client only moves forward. */
    record Sync(String sessionId, int tick) implements IntroPayload {
        public static final Type<Sync> TYPE = IntroPayload.payloadType("sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.ofMember((p, b) -> { b.writeUtf(p.sessionId); b.writeVarInt(p.tick); }, b -> new Sync(b.readUtf(), b.readVarInt()));
        @Override public Type<Sync> type() { return TYPE; }
    }

    record Reveal(String sessionId, UUID uuid, String name) implements IntroPayload {
        public static final Type<Reveal> TYPE = IntroPayload.payloadType("reveal");
        public static final StreamCodec<RegistryFriendlyByteBuf, Reveal> CODEC =
                StreamCodec.ofMember((p, b) -> { b.writeUtf(p.sessionId); b.writeUUID(p.uuid); b.writeUtf(p.name); },
                        b -> new Reveal(b.readUtf(), b.readUUID(), b.readUtf()));
        @Override public Type<Reveal> type() { return TYPE; }
    }

    record End(String sessionId, String reason) implements IntroPayload {
        public static final Type<End> TYPE = IntroPayload.payloadType("end");
        public static final StreamCodec<RegistryFriendlyByteBuf, End> CODEC =
                StreamCodec.ofMember((p, b) -> { b.writeUtf(p.sessionId); b.writeUtf(p.reason); }, b -> new End(b.readUtf(), b.readUtf()));
        @Override public Type<End> type() { return TYPE; }
    }

    /** Client -> server: renderer/shader problems. The server only logs it; restore never depends on a client. */
    record ClientStatus(String sessionId, boolean ok, String message) implements IntroPayload {
        public static final Type<ClientStatus> TYPE = IntroPayload.payloadType("client_status");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientStatus> CODEC =
                StreamCodec.ofMember((p, b) -> { b.writeUtf(p.sessionId); b.writeBoolean(p.ok); b.writeUtf(p.message); },
                        b -> new ClientStatus(b.readUtf(), b.readBoolean(), b.readUtf()));
        @Override public Type<ClientStatus> type() { return TYPE; }
    }
}
