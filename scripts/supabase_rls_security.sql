-- ==============================================================================
-- SUPABASE ROW LEVEL SECURITY (RLS) HARDENING SCRIPT FOR PULSE / SIMPMUSIC
-- ==============================================================================
-- This script hardens database tables against unauthorized access, user spoofing,
-- cross-tenant data leaks, and unauthenticated public tampering.
--
-- Tables secured:
--   1. public.user_liked_songs
--   2. public.user_playlists
--   3. public.user_listen_history
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. TABLE CREATION / STRUCTURAL INTEGRITY
-- ------------------------------------------------------------------------------

-- 1.1 user_liked_songs
CREATE TABLE IF NOT EXISTS public.user_liked_songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE DEFAULT auth.uid(),
    song_id TEXT NOT NULL,
    song_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_liked_song UNIQUE (user_id, song_id)
);

-- 1.2 user_playlists
CREATE TABLE IF NOT EXISTS public.user_playlists (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE DEFAULT auth.uid(),
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    songs JSONB DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 1.3 user_listen_history
CREATE TABLE IF NOT EXISTS public.user_listen_history (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE DEFAULT auth.uid(),
    song_id TEXT NOT NULL,
    song_data JSONB,
    play_count INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT unique_user_song_history UNIQUE (user_id, song_id)
);

-- Ensure user_id defaults and constraints if tables already existed
ALTER TABLE public.user_liked_songs ALTER COLUMN user_id SET DEFAULT auth.uid();
ALTER TABLE public.user_liked_songs ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE public.user_playlists ALTER COLUMN user_id SET DEFAULT auth.uid();
ALTER TABLE public.user_playlists ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE public.user_listen_history ALTER COLUMN user_id SET DEFAULT auth.uid();
ALTER TABLE public.user_listen_history ALTER COLUMN user_id SET NOT NULL;

-- ------------------------------------------------------------------------------
-- 2. ENABLE AND FORCE ROW LEVEL SECURITY (RLS)
-- ------------------------------------------------------------------------------
-- 'ENABLE ROW LEVEL SECURITY' applies RLS to normal roles.
-- 'FORCE ROW LEVEL SECURITY' ensures even table owners cannot bypass policies.

ALTER TABLE public.user_liked_songs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_liked_songs FORCE ROW LEVEL SECURITY;

ALTER TABLE public.user_playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_playlists FORCE ROW LEVEL SECURITY;

ALTER TABLE public.user_listen_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_listen_history FORCE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 3. ROLE-LEVEL PERMISSIONS (LEAST PRIVILEGE)
-- ------------------------------------------------------------------------------
-- Revoke all direct permissions from the unauthenticated 'anon' and 'public' roles.
-- Only 'authenticated' role users receive CRUD permissions (governed by RLS).

REVOKE ALL ON TABLE public.user_liked_songs FROM anon, public;
REVOKE ALL ON TABLE public.user_playlists FROM anon, public;
REVOKE ALL ON TABLE public.user_listen_history FROM anon, public;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_liked_songs TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_playlists TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_listen_history TO authenticated;

-- ------------------------------------------------------------------------------
-- 4. CLEAN UP EXISTING POLICIES
-- ------------------------------------------------------------------------------
-- Drop any legacy or loosely configured policies to guarantee clean slate.

DROP POLICY IF EXISTS "Users own their liked songs" ON public.user_liked_songs;
DROP POLICY IF EXISTS "user_liked_songs_select_policy" ON public.user_liked_songs;
DROP POLICY IF EXISTS "user_liked_songs_insert_policy" ON public.user_liked_songs;
DROP POLICY IF EXISTS "user_liked_songs_update_policy" ON public.user_liked_songs;
DROP POLICY IF EXISTS "user_liked_songs_delete_policy" ON public.user_liked_songs;

DROP POLICY IF EXISTS "Users own their playlists" ON public.user_playlists;
DROP POLICY IF EXISTS "user_playlists_select_policy" ON public.user_playlists;
DROP POLICY IF EXISTS "user_playlists_insert_policy" ON public.user_playlists;
DROP POLICY IF EXISTS "user_playlists_update_policy" ON public.user_playlists;
DROP POLICY IF EXISTS "user_playlists_delete_policy" ON public.user_playlists;

DROP POLICY IF EXISTS "Users own their listen history" ON public.user_listen_history;
DROP POLICY IF EXISTS "user_listen_history_select_policy" ON public.user_listen_history;
DROP POLICY IF EXISTS "user_listen_history_insert_policy" ON public.user_listen_history;
DROP POLICY IF EXISTS "user_listen_history_update_policy" ON public.user_listen_history;
DROP POLICY IF EXISTS "user_listen_history_delete_policy" ON public.user_listen_history;

-- ------------------------------------------------------------------------------
-- 5. GRANULAR ROW LEVEL SECURITY POLICIES (TO authenticated)
-- ------------------------------------------------------------------------------

-- 5.1 Policies for public.user_liked_songs
CREATE POLICY "user_liked_songs_select_policy"
    ON public.user_liked_songs
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "user_liked_songs_insert_policy"
    ON public.user_liked_songs
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_liked_songs_update_policy"
    ON public.user_liked_songs
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_liked_songs_delete_policy"
    ON public.user_liked_songs
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- 5.2 Policies for public.user_playlists
CREATE POLICY "user_playlists_select_policy"
    ON public.user_playlists
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "user_playlists_insert_policy"
    ON public.user_playlists
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_playlists_update_policy"
    ON public.user_playlists
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_playlists_delete_policy"
    ON public.user_playlists
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- 5.3 Policies for public.user_listen_history
CREATE POLICY "user_listen_history_select_policy"
    ON public.user_listen_history
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "user_listen_history_insert_policy"
    ON public.user_listen_history
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_listen_history_update_policy"
    ON public.user_listen_history
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "user_listen_history_delete_policy"
    ON public.user_listen_history
    FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 6. DEFENSE-IN-DEPTH: ANTI-SPOOFING & INTEGRITY TRIGGERS
-- ------------------------------------------------------------------------------
-- Automatically binds user_id to auth.uid() on INSERT regardless of client payload,
-- and forbids re-assigning user_id on UPDATE.

CREATE OR REPLACE FUNCTION public.enforce_rls_user_id()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF auth.uid() IS NULL THEN
            RAISE EXCEPTION 'RLS Security Error: Unauthenticated insert attempt.';
        END IF;
        NEW.user_id := auth.uid();
    ELSIF TG_OP = 'UPDATE' THEN
        IF NEW.user_id <> OLD.user_id THEN
            RAISE EXCEPTION 'RLS Security Error: Changing user_id ownership is prohibited.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_enforce_user_id_liked_songs ON public.user_liked_songs;
CREATE TRIGGER trg_enforce_user_id_liked_songs
    BEFORE INSERT OR UPDATE ON public.user_liked_songs
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_rls_user_id();

DROP TRIGGER IF EXISTS trg_enforce_user_id_playlists ON public.user_playlists;
CREATE TRIGGER trg_enforce_user_id_playlists
    BEFORE INSERT OR UPDATE ON public.user_playlists
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_rls_user_id();

DROP TRIGGER IF EXISTS trg_enforce_user_id_listen_history ON public.user_listen_history;
CREATE TRIGGER trg_enforce_user_id_listen_history
    BEFORE INSERT OR UPDATE ON public.user_listen_history
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_rls_user_id();

-- ------------------------------------------------------------------------------
-- 7. PERFORMANCE & DOS-PREVENTION INDEXES
-- ------------------------------------------------------------------------------
-- Indexes on user_id ensure RLS predicate auth.uid() = user_id executes via
-- index scan in O(1) instead of sequential table scans.

CREATE INDEX IF NOT EXISTS idx_user_liked_songs_user_id ON public.user_liked_songs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_playlists_user_id ON public.user_playlists(user_id);
CREATE INDEX IF NOT EXISTS idx_user_listen_history_user_id ON public.user_listen_history(user_id);

-- ------------------------------------------------------------------------------
-- 8. ANALYTICAL DATA STORE (PULSE MUSIC ANALYTICS)
-- ------------------------------------------------------------------------------
-- Audio API remains strictly decoupled and independent.
-- User listening & app usage analytics are stored securely in Supabase under RLS.

CREATE TABLE IF NOT EXISTS public.user_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE DEFAULT auth.uid(),
    event_type TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_analytics ALTER COLUMN user_id SET DEFAULT auth.uid();
ALTER TABLE public.user_analytics ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE public.user_analytics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_analytics FORCE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.user_analytics FROM anon, public;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_analytics TO authenticated;

DROP POLICY IF EXISTS "user_analytics_select_policy" ON public.user_analytics;
CREATE POLICY "user_analytics_select_policy"
    ON public.user_analytics
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "user_analytics_insert_policy" ON public.user_analytics;
CREATE POLICY "user_analytics_insert_policy"
    ON public.user_analytics
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP TRIGGER IF EXISTS trg_enforce_user_id_analytics ON public.user_analytics;
CREATE TRIGGER trg_enforce_user_id_analytics
    BEFORE INSERT OR UPDATE ON public.user_analytics
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_rls_user_id();

CREATE INDEX IF NOT EXISTS idx_user_analytics_user_id ON public.user_analytics(user_id);

-- ==============================================================================
-- END OF RLS SECURITY HARDENING SCRIPT
-- ==============================================================================

