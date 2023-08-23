package ru.blogic.muzedodevwebutils.api.front;

import io.vavr.Predicates;
import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.command.CommandDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FrontendService {
    CommandDao commandDao;
    MuzedoServerDao muzedoServerDao;

    @NonFinal
    @Value("${app.version}")
    String actualVersion;

    @DisableLoggingAspect
    public GetFrontendConfigResponse getFrontendConfig() {
        return new GetFrontendConfigResponse(
            this.actualVersion,
            commandDao.getAll()
                .filter(Predicates.not(Command::hidden))
                .map(c -> new GetFrontendConfigResponse.GetFrontendConfigResponseCommand(
                    c.id(),
                    c.name(),
                    c.blocksWsadmin()
                )),
            muzedoServerDao.getAll().map(MuzedoServer::getId)
        );
    }


    public record GetFrontendConfigResponse(
        String version,
        List<GetFrontendConfigResponseCommand> commands,
        List<Integer> servers
    ) {

        public record GetFrontendConfigResponseCommand(
            String id,
            String name,
            boolean blocksWsadmin
        ){}
    }
}
