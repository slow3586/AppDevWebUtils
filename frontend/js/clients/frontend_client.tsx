import {getWrapper} from "../utils/client";

export type FrontendConfigCommand = {
    id: string,
    name: string,
    blocksWsadmin: boolean
}

export type GetFrontendConfigResponse = {
    version: string,
    commands: FrontendConfigCommand[],
    servers: number[]
}

export const getFrontendConfig = (
): Promise<GetFrontendConfigResponse> =>
    getWrapper(`api/front`)