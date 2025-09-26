use std::sync::Arc;
use smithay::{
    reexports::{
        calloop::{
            generic::Generic as GenericEvent,
            self, EventLoop,
        },
        wayland_server::{
            self,
            backend::{ClientData, ClientId, DisconnectReason},
            protocol::{
                wl_surface::WlSurface,
                wl_buffer::WlBuffer,
            },
            Display, DisplayHandle,
        },
    },
    wayland::{
        socket::ListeningSocketSource,
        compositor::{CompositorState, CompositorClientState, CompositorHandler},
        buffer::BufferHandler,
        shm::{ShmState, ShmHandler},
    },
    delegate_compositor, delegate_shm,
};

pub struct WLCState {
    pub display_handle: DisplayHandle,
    pub compositor_state: CompositorState,
    pub shm_state: ShmState,
}

impl WLCState {
    pub fn new(disp: DisplayHandle) -> Self {
        Self {
            display_handle: disp.clone(),
            compositor_state: CompositorState::new::<WLCState>(&disp),
            shm_state: ShmState::new::<WLCState>(&disp, vec![]),
        }
    }
}

impl CompositorHandler for WLCState {
    fn compositor_state(&mut self) -> &mut CompositorState {
        &mut self.compositor_state
    }

    fn client_compositor_state<'a>(
        &self,
        client: &'a wayland_server::Client,
    ) -> &'a CompositorClientState {
        &client.get_data::<WLCClient>().unwrap().compositor_state
    }

    fn commit(&mut self, surface: &WlSurface) {
        println!("Client commited surface: {:?}", surface);
    }
}

impl BufferHandler for WLCState {
    fn buffer_destroyed(&mut self, _buffer: &WlBuffer) {
    }
}

impl ShmHandler for WLCState {
    fn shm_state(&self) -> &ShmState {
        &self.shm_state
    }
}

pub struct WLCClient {
    compositor_state: CompositorClientState,
}

impl WLCClient {
    fn new() -> Self {
        Self {
            compositor_state: CompositorClientState::default(),
        }
    }
}

impl ClientData for WLCClient {
    fn initialized(&self, _id: ClientId) {
        println!("Client connected!");
    }

    fn disconnected(&self, _id: ClientId, _reason: DisconnectReason) {
        println!("Client disconnected!");
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Hello, world!");

    let mut event_loop: EventLoop<WLCState> = EventLoop::try_new()?;
    let display: Display<WLCState> = Display::new()?;
    let socket = ListeningSocketSource::new_auto()?;

    println!("Listening on: '{}'", socket.socket_name().to_str().unwrap());

    let mut state = WLCState::new(display.handle());
    let ev_handle = event_loop.handle();

    ev_handle.insert_source(socket, |stream, _, state| {
        let client = WLCClient::new();
        state.display_handle.insert_client(stream, Arc::new(client)).unwrap();
    }).unwrap();

    let display_source = GenericEvent::new(
        display, calloop::Interest::READ, calloop::Mode::Level
    );
    ev_handle.insert_source(display_source, |_, display_io, state| {
        unsafe {
            display_io.get_mut().dispatch_clients(state).unwrap();
        }
        Ok(calloop::PostAction::Continue)
    }).unwrap();

    loop {
        event_loop.dispatch(None, &mut state).unwrap();
        state.display_handle.flush_clients().unwrap();
    }
}

delegate_compositor!(WLCState);
delegate_shm!(WLCState);
